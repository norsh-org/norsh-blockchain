package org.norsh.blockchain.services.queue;

import org.norsh.exceptions.OperationStatus;
import org.norsh.model.transport.DataTransfer;

public class MessagingResponseService {
////	private final CacheService cacheService;
////	private final BlockchainConfig blockchainConfig = BlockchainConfig.getInstance();
//
//	@Autowired
//	public MessagingResponseService(CacheService cacheService) {
//		//this.cacheService = cacheService;
//	}

	public DataTransfer response(String requestId, OperationStatus status, Object data) {
		DataTransfer dataTransfer = new DataTransfer(requestId, status, data);
		//cacheService.save(dataTransfer.getRequestId(), dataTransfer, blockchainConfig.getDefaultsConfig().getMessagingTtlMs());
		return dataTransfer;
	}
	
	public DataTransfer response(String request, Object data) {
		return response(request, OperationStatus.OK, data);
	}
}
