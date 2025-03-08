package org.norsh.blockchain.services.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.conversions.Bson;
import org.norsh.blockchain.S;
import org.norsh.blockchain.components.NorshConstants;
import org.norsh.blockchain.model.elements.ElementDoc;
import org.norsh.blockchain.model.elements.ElementStatus;
import org.norsh.blockchain.model.utils.DynamicSequenceDoc;
import org.norsh.blockchain.services.queue.MessagingResponseService;
import org.norsh.exceptions.OperationException;
import org.norsh.exceptions.OperationStatus;
import org.norsh.model.dtos.elements.ElementCreateDto;
import org.norsh.model.dtos.elements.ElementGetDto;
import org.norsh.model.dtos.elements.ElementMetadataDto;
import org.norsh.model.transport.DataTransfer;
import org.norsh.model.transport.Processable;
import org.norsh.rest.RestMethod;
import org.norsh.security.Hasher;
import org.norsh.util.Converter;
import org.norsh.util.Strings;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

/**
 * Smart Element Management Service.
 * <p>
 * This service handles the creation, retrieval, update, and metadata management of Smart Elements. It enforces business
 * rules, ensures transactional integrity via distributed semaphores, and securely stores elements in MongoDB. It also
 * communicates with external services via message queues.
 * </p>
 *
 * <h2>Main Responsibilities:</h2>
 * <ul>
 * <li>Create Smart Elements with transactional integrity.</li>
 * <li>Retrieve Smart Elements by ID.</li>
 * <li>Update metadata and policies according to business rules.</li>
 * <li>Manage network associations for elements, specifically for PROXY type elements.</li>
 * </ul>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
//@Processable({ElementCreateDto.class, ElementGetDto.class, ElementMetadataDto.class, ElementNetworkDto.class})
public class ElementService {
	private MessagingResponseService messagingService = new MessagingResponseService();


	/**
	 * Retrieves a Smart Element by its ID.
	 *
	 * @param dto the {@link ElementGetDto} containing the unique element ID.
	 * @return a {@link DataTransfer} object encapsulating the element data, or a {@link OperationStatus#NOT_FOUND} status
	 *         if the element is not found.
	 */
	@Processable(method = RestMethod.GET)
	public DataTransfer getElement(ElementGetDto dto) {
		ElementDoc element = S.elementTemplate.id(dto.getId());

		if (element != null) {
			return messagingService.response(dto.getId(), element);
		} else {
			return messagingService.response(dto.getId(), OperationStatus.NOT_FOUND);
		}
	}

	/**
	 * Creates a new Smart Element.
	 * <p>
	 * This method validates the input data, checks if an element with the same hash already exists, and initiates a
	 * transaction to create the element. It uses a distributed semaphore to ensure the atomic assignment of a unique
	 * identifier.
	 * </p>
	 * <p>
	 * As part of the process, a charge transaction is executed with structured reference information. The reference info is
	 * set to "ELEMENT|CREATE" to indicate that the operation is an element creation.
	 * </p>
	 *
	 * @param dto the {@link ElementCreateDto} containing all necessary details for element creation.
	 * @return a {@link DataTransfer} object with the creation result. On success, returns the element data; otherwise,
	 *         returns an error or already exists status.
	 */
	@Processable(method = RestMethod.POST)
	public ElementDoc createElement(ElementCreateDto dto) {
		dto.validate();

		// Check if the element already exists.
		if (S.elementTemplate.exists(Filters.eq("hash", dto.getHash())))
			throw new OperationException(OperationStatus.EXISTS, "Element exists");

		String owner = Hasher.sha3Hex(Converter.base64OrHexToBytes(dto.getPublicKey()));
		
		// Create and initialize the Smart Element.
		ElementDoc element = new ElementDoc();
		element.setType(dto.getType());
		element.setOwner(owner);
		element.setSymbol(dto.getSymbol());
		element.setDecimals(dto.getDecimals());
		element.setInitialSupply(dto.getInitialSupply());
		element.setTfo(dto.getTfo());
		element.setPublicKey(dto.getPublicKey());
		element.setHash(dto.getHash());
		element.setSignature(dto.getSignature());
		element.setTimestamp(System.currentTimeMillis());
		element.setVersion(1);
		element.setPrivacy(false);
		element.setStatus(ElementStatus.PENDING);

		// Save the Smart Element using a semaphore lock to ensure atomic updates and consistency.
		S.semaphoreService.execute(NorshConstants.getTagElements(), _ -> {
			DynamicSequenceDoc dynamicSequence = S.dynamicSequenceService.get(NorshConstants.getTagElements());
			element.setPreviousId(dynamicSequence.getData());
			element.setId(Hasher.sha3Hex(Strings.concatenate(element.getPreviousId(), element.getHash(), element.getTimestamp())));
			
//			PaymentCreateDto paymentCreate = new PaymentCreateDto();
//			paymentCreate.setTo(S.norshCoin.getOwner());
//			paymentCreate.setElement(S.norshCoin.getId());
//			paymentCreate.setVolume(FeePolicy.getElementCreateAmount());
//			paymentCreate.setNonce(element.getTimestamp());
//			paymentCreate.setPublicKey(element.getPublicKey());
//			paymentCreate.setLink(element.getId());
//
//			//S.paymentService.internalTransaction(paymentCreate);
			S.elementTemplate.save(element);
			
			S.dynamicSequenceService.set(NorshConstants.getTagElements(), element.getId());
		});

		return S.elementTemplate.id(element.getId());
	}

	/**
	 * Updates the metadata of a Smart Element.
	 * <p>
	 * This method validates the metadata update request and ensures that the element exists. It applies updates based on
	 * the following rules:
	 * </p>
	 * <ul>
	 * <li>{@code null}: Field is not modified.</li>
	 * <li>{@code ""} (empty string): Field is removed (unset in MongoDB).</li>
	 * <li>Any other value: Field is updated with the provided value.</li>
	 * </ul>
	 * <p>
	 * If the element already contains metadata, a charge transaction is executed. The transaction's reference information
	 * is set to "ELEMENT|METADATA|UPDATE" to indicate a metadata update operation.
	 * </p>
	 *
	 * @param dto the {@link ElementMetadataDto} containing the metadata update details.
	 * @return a {@link DataTransfer} object with the updated element data or an error status if validation fails.
	 */
	@Processable(method = RestMethod.PUT)
	public DataTransfer setMetadata(ElementMetadataDto dto) {
		ElementDoc element = S.elementTemplate.id(dto.getId());

		try {
			dto.validate();
		} catch (Exception ex) {
			return messagingService.response(dto.getHash(), OperationStatus.ERROR, ex.getMessage());
		}

		String owner = Hasher.sha3Hex(Converter.base64OrHexToBytes(dto.getPublicKey()));
		if (element == null) {
			return messagingService.response(dto.getHash(), OperationStatus.NOT_FOUND);
		} else if (!element.getOwner().equals(owner)) {
			return messagingService.response(dto.getHash(), OperationStatus.FORBIDDEN);
		}

		// Execute a charge transaction for the metadata update if metadata already exists.
		if (element.getMetadata() != null) {
			try {
				// dto.validateTransaction();
			} catch (Exception ex) {
				return messagingService.response(dto.getHash(), OperationStatus.ERROR, ex.getMessage());
			}

			DataTransfer transactionTransport = S.transactionService.createTransaction(dto.getTransaction(), e -> { e.setMetadata(Map.of("element", element.getId(), "action", "UPDATE", "child", "metadata")); });

			if (transactionTransport.getStatus() != OperationStatus.OK) {
				return transactionTransport;
			}
		}

		// Utility to set updates based on provided data.
		var updateUtil = new Object() {
			void set(List<Bson> updates, String field, String value) {
				if (value != null) {
					if (value.isBlank()) {
						updates.add(Updates.unset(field)); // Remove field if empty.
					} else {
						updates.add(Updates.set(field, value)); // Update field if not empty.
					}
				}
			}
		};

		// Build the update operation for metadata fields.
		List<Bson> updates = new ArrayList<>();
		//Update update = new Update();
		updateUtil.set(updates, "metadata.name", dto.getName());
		updateUtil.set(updates, "metadata.about", dto.getAbout());
		updateUtil.set(updates, "metadata.logo", dto.getLogo());
		updateUtil.set(updates, "metadata.site", dto.getSite());
		updateUtil.set(updates, "metadata.policy", dto.getPolicy());

		S.elementTemplate.update(Filters.eq("_id", dto.getId()), Updates.combine(updates.toArray(new Bson[0])));
		
		return messagingService.response(dto.getHash(), S.elementTemplate.id(dto.getId()));
	}

//	/**
//	 * Updates the policy of a Smart Element.
//	 * <p>
//	 * This method validates the update request and applies changes to the element's policy fields. If the policy already
//	 * exists, updating it may require an associated charge transaction. The transaction's reference information is set to
//	 * "ELEMENT|POLICY|UPDATE" to indicate a policy update operation.
//	 * </p>
//	 *
//	 * @param dto the {@link ElementPolicyDto} containing the new policy details.
//	 * @return a {@link DataTransfer} object with the updated element data or an error status if the update fails.
//	 */
//	@Processable(method = RestMethod.PUT)
//	public DataTransfer setPolicy(ElementPolicyDto dto) {
//		ElementDoc element = elementTemplate.id(dto.getId());
//
//		try {
//			dto.validate();
//		} catch (Exception ex) {
//			return messagingService.response(dto.getHash(), OperationStatus.ERROR, ex.getMessage());
//		}
//
//		String owner = Hasher.sha3Hex(Converter.base64OrHexToBytes(dto.getPublicKey()));
//		if (element == null) {
//			return messagingService.response(dto.getHash(), OperationStatus.NOT_FOUND);
//		} else if (!element.getOwner().equals(owner)) {
//			return messagingService.response(dto.getHash(), OperationStatus.FORBIDDEN);
//		}
//
//		// Execute a charge transaction for the policy update if the policy already exists.
//		if (element.getPolicy() != null) {
//			try {
//				// dto.validateTransaction();
//			} catch (Exception ex) {
//				return messagingService.response(dto.getHash(), OperationStatus.ERROR, ex.getMessage());
//			}
//
//			DataTransfer transactionTransport = transactionService.createTransaction(dto.getTransaction(), e -> { e.setMetadata(Map.of("element", element.getId(), "action", "UPDATE", "child", "policy")); });
//
//			if (transactionTransport.getStatus() != OperationStatus.OK) {
//				return transactionTransport;
//			}
//		}
//
//		// Utility to set updates on policy fields.
//		var updateUtil = new Object() {
//			void set(Update update, String field, Object value) {
//				if (value != null) {
//					if (value.toString().isBlank()) {
//						update.unset(field); // Remove field if empty.
//					} else {
//						update.set(field, value); // Update field if not empty.
//					}
//				}
//			}
//		};
//
//		// Build the update operation for policy fields.
//		Update update = new Update();
//		updateUtil.set(update, "policy.transactionTax", dto.getTransactionTax());
//		updateUtil.set(update, "policy.freezeDuration", dto.getFreezeDuration());
//		updateUtil.set(update, "policy.script", dto.getScript());
//
//		// Execute the update in MongoDB.
//		Criteria criteria = Criteria.where("_id").is(dto.getId());
//		mongoMain.update(criteria, update, ElementDoc.class);
//
//		return messagingService.response(dto.getHash(), mongoMain.id(dto.getId(), ElementDoc.class));
//	}
//
//	/**
//	 * Updates or adds a network association for a Smart Element.
//	 *
//	 * @param dto the {@link ElementNetworkDto} containing the network association details.
//	 * @return a {@link DataTransfer} object with the updated element data or an error status.
//	 */
//	@Processable(method = RestMethod.PUT)
//	public DataTransfer setNetwork(ElementNetworkDto dto) {
//		return setNetwork(dto, false);
//	}
//
//	/**
//	 * Removes a network association from a Smart Element.
//	 *
//	 * @param dto the {@link ElementNetworkDto} containing the network association details.
//	 * @return a {@link DataTransfer} object with the updated element data or an error status.
//	 */
//	@Processable(method = RestMethod.DELETE)
//	public DataTransfer deleteNetwork(ElementNetworkDto dto) {
//		return setNetwork(dto, true);
//	}
//
//	/**
//	 * Internal method to update or remove a network association for a Smart Element.
//	 * <p>
//	 * This method is used by both {@link #setNetwork(ElementNetworkDto)} and {@link #deleteNetwork(ElementNetworkDto)} to
//	 * modify the network association for PROXY type elements. It validates that the element exists and is of type PROXY. If
//	 * monitored networks exist, a charge transaction is executed. The transaction's reference information is set to
//	 * "ELEMENT|NETWORK|UPDATE" to indicate a network update operation.
//	 * </p>
//	 *
//	 * @param dto    the {@link ElementNetworkDto} containing network details.
//	 * @param delete if {@code true}, removes the association; if {@code false}, adds/updates the association.
//	 * @return a {@link DataTransfer} object with the updated element data or an error status.
//	 */
//	private DataTransfer setNetwork(ElementNetworkDto dto, Boolean delete) {
//		ElementDoc element = elementTemplate.id(dto.getId());
//
//		try {
//			dto.validate();
//		} catch (Exception ex) {
//			return messagingService.response(dto.getHash(), OperationStatus.ERROR, ex.getMessage());
//		}
//
//		String owner = Hasher.sha3Hex(Converter.base64OrHexToBytes(dto.getPublicKey()));
//		if (element == null) {
//			return messagingService.response(dto.getHash(), OperationStatus.NOT_FOUND);
//		} else if (!element.getOwner().equals(owner)) {
//			return messagingService.response(dto.getHash(), OperationStatus.FORBIDDEN);
//		} else if (element.getType() != ElementType.PROXY) {
//			return messagingService.response(dto.getHash(), OperationStatus.ERROR, "This element is not a proxy");
//		}
//
//		// Execute a charge transaction for the network update if monitored networks exist.
//		if (element.getMonitoredNetworks() != null) {
//			try {
//				// dto.validateTransaction();
//			} catch (Exception ex) {
//				return messagingService.response(dto.getHash(), OperationStatus.ERROR, ex.getMessage());
//			}
//
//			DataTransfer transactionTransport = transactionService.createTransaction(dto.getTransaction(), e -> { e.setMetadata(Map.of("element", element.getId(), "action", "UPDATE", "child", "network")); });
//
//			if (transactionTransport.getStatus() != OperationStatus.OK) {
//				return transactionTransport;
//			}
//		}
//
//		// Build the update operation for the network association.
//		Update update = new Update();
//		if (delete) {
//			update.unset("monitoredNetworks." + dto.getAddress());
//		} else {
//			update.set("monitoredNetworks." + dto.getAddress(), dto.getNetwork());
//		}
//
//		// Execute the update in MongoDB.
//		Criteria criteria = Criteria.where("_id").is(dto.getId());
//		mongoMain.update(criteria, update, ElementDoc.class);
//
//		return messagingService.response(dto.getHash(), mongoMain.id(dto.getId(), ElementDoc.class));
//	}
}
