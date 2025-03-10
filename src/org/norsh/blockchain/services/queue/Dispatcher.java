package org.norsh.blockchain.services.queue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.norsh.blockchain.S;
import org.norsh.exceptions.InternalException;
import org.norsh.exceptions.OperationException;
import org.norsh.model.dtos.DistributedDto;
import org.norsh.model.transport.DataTransfer;
import org.norsh.model.transport.Processable;
import org.norsh.rest.RestMethod;
import org.norsh.util.Converter;

/**
 * Dispatcher service for processing distributed DTOs received via queues.
 * <p>
 * This service dynamically maps DTOs to their corresponding service methods using annotations. When a new request
 * arrives, it dispatches it to the correct processing method.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 * <li>Uses {@link Processable} annotations to map DTOs to processing methods.</li>
 * <li>Automatically discovers and registers service methods.</li>
 * <li>Supports dispatching based on {@link RestMethod} (e.g., POST, PUT, GET).</li>
 * <li>Handles dynamic conversion of DTOs from JSON.</li>
 * </ul>
 *
 * @since 1.0.0
 * @version 1.0.0
 */

public class Dispatcher {
	private Map<String, Method> methodRegistry = null;
	private Map<String, Object> serviceRegistry = null;

	private final MessagingResponseService messagingService = new MessagingResponseService();

	/**
	 * Loads and caches all processable service methods annotated with {@link Processable}.
	 * <p>
	 * This method ensures that the dispatcher is initialized only when needed, avoiding circular dependencies during Spring
	 * bootstrap.
	 * </p>
	 * 
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	public void loadDispatcherService() {
		try {
			methodRegistry = new ConcurrentHashMap<>();
			serviceRegistry = new ConcurrentHashMap<>();

			// Scan all Spring services and register annotated methods
			for (Field field : S.class.getFields()) {
				Object service = field.get(null);
				
				if (service == null)
					continue;

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
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Dispatches an incoming JSON request to the corresponding processing method.
	 *
	 * @param json The JSON string containing the transport request.
	 * @throws InternalException if no matching processor is found or an error occurs during execution.
	 */
	public void dispatch(String json) {
		dispatch(Converter.fromJson(json, DataTransfer.class));
	}

	public DataTransfer dispatch(DataTransfer transfer) {
		try {
			String requestClassName = transfer.getRequestClassName();
			String key = generateKey(requestClassName, transfer.getMethod());

			// Validate that the class belongs to the DistributedDto package
			if (requestClassName != null && requestClassName.startsWith(DistributedDto.class.getPackageName())) {
				DistributedDto dto = (DistributedDto) Converter.convert(transfer.getRequestData(), Class.forName(requestClassName));

				// Retrieve the registered processing method
				Method method = methodRegistry.get(key);
				if (method == null) {
					S.log.warning("No processor found for key: " + key);
					throw new InternalException("No processor found for: " + key);
				}

				// Retrieve the service instance and invoke the processing method
				Object serviceInstance = serviceRegistry.get(method.getDeclaringClass().getCanonicalName());
				try {
					System.out.println("method: " + method);

					Object result = method.invoke(serviceInstance, dto);
					System.out.println("RESULT: " + result + "\n\n\n");
					
					return messagingService.response(dto.getRequestId(), result);
				} catch (InvocationTargetException ex) {
					ex.printStackTrace();
					
					if (ex.getCause() instanceof OperationException) {
						OperationException ox = (OperationException) ex.getCause();
						return messagingService.response(dto.getRequestId(), ox.getOperationStatus(), ox.getData());
					} else {
						return null;
					}
				}
			} else {
				S.log.warning("Invalid DTO class: " + requestClassName);
				throw new InternalException("Invalid DTO class: " + requestClassName);
			}
		} catch (Exception e) {
			e.printStackTrace();
			S.log.error("Error processing request: ", e);
			throw new InternalException("Error processing request: ", e);
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
