package org.norsh.blockchain.v1;

import org.norsh.blockchain.services.queue.DispatcherService;
import org.norsh.model.transport.DataTransfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Abstract base class for API version 1 controllers.
 * <p>
 * This class serves as the foundation for all API controllers in version 1, ensuring compatibility, future evolution,
 * reusability, and standardization across the Norsh platform.
 * </p>
 *
 * <h3>Purpose:</h3>
 * <ul>
 * <li>Provides a unified base for version 1 API controllers.</li>
 * <li>Facilitates consistent behaviors and patterns for APIs.</li>
 * <li>Allows for seamless upgrades and compatibility with future API versions.</li>
 * </ul>
 *
 * <h3>Key Features:</h3>
 * <ul>
 * <li>Ensures API controllers adhere to a standardized structure.</li>
 * <li>Simplifies future extensibility and integration of cross-cutting concerns.</li>
 * </ul>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
@RestController
@RequestMapping("/v1/blockchain")
public class DataTransferApiV1 {
	@Autowired
	protected HttpServletRequest servletRequest;
	
	@Autowired
	private DispatcherService dispatcherService;

	/**
	 * Processes a Smart Element request, forwarding it to the queue and caching its status.
	 *
	 * @param request The unique request identifier.
	 * @param data    The payload to be sent to the processing queue.
	 * @return a response confirming that the request has been accepted for processing.
	 */
	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<Object> processRequest(@RequestBody DataTransfer transfer) {
		DataTransfer result = dispatcherService.dispatch(transfer);
		return ResponseEntity.status(HttpStatus.OK).body(result);
	}
}
