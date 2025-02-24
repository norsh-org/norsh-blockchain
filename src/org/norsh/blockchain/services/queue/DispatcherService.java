package org.norsh.blockchain.services.queue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.norsh.exceptions.InternalException;
import org.norsh.exceptions.OperationException;
import org.norsh.model.dtos.DistributedDto;
import org.norsh.model.transport.DataTransfer;
import org.norsh.model.transport.Processable;
import org.norsh.model.transport.RestMethod;
import org.norsh.util.Converter;
import org.norsh.util.Log;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Dispatcher service for processing distributed DTOs received via queues.
 * <p>
 * This service dynamically maps DTOs to their corresponding service methods using annotations. 
 * When a new request arrives, it dispatches it to the correct processing method.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Uses {@link Processable} annotations to map DTOs to processing methods.</li>
 *   <li>Automatically discovers and registers service methods.</li>
 *   <li>Supports dispatching based on {@link RestMethod} (e.g., POST, PUT, GET).</li>
 *   <li>Handles dynamic conversion of DTOs from JSON.</li>
 * </ul>
 *
 * @since 1.0.0
 * @version 1.0.0
 */
@Service
public class DispatcherService {
	private Map<String, Method> methodRegistry = null;
	private Map<String, Object> serviceRegistry = null;

	private final MessagingResponseService messagingService;
	private final ApplicationContext context;
	private final Log log;

	/**
	 * Constructs a new {@link DispatcherService}.
	 *
	 * @param context The Spring {@link ApplicationContext} to scan for service beans.
	 */
	public DispatcherService(MessagingResponseService messagingService, ApplicationContext context, Log log) {
		this.messagingService = messagingService;
		this.context = context;
		this.log = log;
	}

	/**
	 * Loads and caches all processable service methods annotated with {@link Processable}.
	 * <p>
	 * This method ensures that the dispatcher is initialized only when needed, avoiding 
	 * circular dependencies during Spring bootstrap.
	 * </p>
	 */
	private synchronized void loadDispatcherService() {
		if (methodRegistry != null)
			return;

		methodRegistry = new ConcurrentHashMap<>();
		serviceRegistry = new ConcurrentHashMap<>();

		// Scan all Spring services and register annotated methods
		for (Object service : context.getBeansWithAnnotation(Service.class).values()) {
			Class<?> serviceClass = service.getClass();
			
			for (Method method : serviceClass.getDeclaredMethods()) {
				if (method.isAnnotationPresent(Processable.class)) {
					Processable annotation = method.getAnnotation(Processable.class);

					// Ensure the method has exactly one parameter (DTO)
					if (method.getParameterCount() == 1) {
						Class<?> parameter = method.getParameterTypes()[0];
						String key = generateKey(parameter.getCanonicalName(), annotation.method());
						methodRegistry.put(key, method);
						
						if (!serviceRegistry.containsKey(serviceClass.getCanonicalName()))
							serviceRegistry.put(serviceClass.getCanonicalName(), service);
					}
				}
			}
		}
		
		log.debug("DispatcherService initialization completed.");
	}

	/**
	 * Dispatches an incoming JSON request to the corresponding processing method.
	 *
	 * @param json The JSON string containing the transport request.
	 * @throws InternalException if no matching processor is found or an error occurs during execution.
	 */
	public void dispatch(String json) {
		loadDispatcherService();

		try {
			DataTransfer transport = Converter.fromJson(json, DataTransfer.class);
			String requestClassName = transport.getRequestClassName();
			String key = generateKey(requestClassName, transport.getMethod());

			// Validate that the class belongs to the DistributedDto package
			if (requestClassName != null && requestClassName.startsWith(DistributedDto.class.getPackageName())) {
				DistributedDto dto = (DistributedDto) Converter.convert(transport.getRequestData(), Class.forName(requestClassName));

				// Retrieve the registered processing method
				Method method = methodRegistry.get(key);
				if (method == null) {
					log.warning("No processor found for key: " + key);
					throw new InternalException("No processor found for: " + key);
				}

				// Retrieve the service instance and invoke the processing method
				Object serviceInstance = serviceRegistry.get(method.getDeclaringClass().getCanonicalName());
				try {
					Object result = method.invoke(serviceInstance, dto);
					messagingService.response(dto.getRequestId(), result);
				} catch (InvocationTargetException ex) {
					if (ex.getCause() instanceof OperationException) {
						OperationException ox = (OperationException) ex.getCause();
						messagingService.response(dto.getRequestId(), ox.getOperationStatus(), ox.getData());
					} else {
						ex.printStackTrace();
					}
				}
			} else {
				log.warning("Invalid DTO class: " + requestClassName);
				throw new InternalException("Invalid DTO class: " + requestClassName);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error processing request: " + json, e);
			throw new InternalException("Error processing request: " + json, e);
		}
	}

	/**
	 * Generates a unique key for mapping DTOs to processing methods.
	 *
	 * @param className The canonical name of the DTO class.
	 * @param method    The HTTP method used for processing.
	 * @return A unique key for method registration.
	 */
	private String generateKey(String className, RestMethod method) {
		return className + ":" + method.name();
	}
}
