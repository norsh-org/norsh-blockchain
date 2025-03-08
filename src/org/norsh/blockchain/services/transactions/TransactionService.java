package org.norsh.blockchain.services.transactions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;

import org.bson.conversions.Bson;
import org.norsh.blockchain.S;
import org.norsh.blockchain.components.NorshConstants;
import org.norsh.blockchain.model.elements.ElementDoc;
import org.norsh.blockchain.model.transactions.BalanceDoc;
import org.norsh.blockchain.model.transactions.TransactionDoc;
import org.norsh.blockchain.model.utils.DynamicSequenceDoc;
import org.norsh.blockchain.services.queue.MessagingResponseService;
import org.norsh.constants.FeePolicy;
import org.norsh.exceptions.OperationException;
import org.norsh.exceptions.OperationStatus;
import org.norsh.model.dtos.transactions.TransactionCreateDto;
import org.norsh.model.dtos.transactions.TransactionGetDto;
import org.norsh.model.transport.DataTransfer;
import org.norsh.model.transport.Processable;
import org.norsh.model.types.TransactionType;
import org.norsh.rest.RestMethod;
import org.norsh.security.Hasher;
import org.norsh.util.Converter;
import org.norsh.util.Shard;
import org.norsh.util.Strings;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

/**
 * Service for handling transaction creation.
 * <p>
 * This service validates transaction requests, verifies the associated element and balance, calculates tax amounts,
 * and stores the transaction in MongoDB using distributed semaphores for consistency. An optional additional execution
 * operation (addOnExecution) can be provided to perform complementary processing alongside the main transaction creation.
 * </p>
 * <p>
 * Process Flow:
 * <ol>
 *   <li>Validate the transaction creation request.</li>
 *   <li>Retrieve the associated element using the Universal Database Notary (UDBN) token.</li>
 *   <li>Calculate and apply tax amounts based on the element's policy and fee settings.</li>
 *   <li>Execute an additional operation for complementary processing if provided.</li>
 *   <li>Verify and update the sender's and recipient's balances using distributed semaphores.</li>
 *   <li>Store the transaction in the ledger by linking it with the previous transaction ID.</li>
 *   <li>Add the transaction to the current block and mark it as confirmed.</li>
 * </ol>
 * </p>
 * 
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
//@Processable({ElementCreateDto.class, ElementGetDto.class, ElementMetadataDto.class, ElementNetworkDto.class})
public class TransactionService {
	private MessagingResponseService messagingService = new MessagingResponseService();

	/**
	 * Retrieves a transaction by its ID.
	 * 
	 * @param dto The {@link TransactionGetDto} containing the unique transaction ID.
	 * @return A {@link DataTransfer} object encapsulating the transaction data, or a NOT_FOUND status if the transaction does not exist.
	 */
	@Processable(method = RestMethod.GET)
	public DataTransfer getTransaction(TransactionGetDto dto) {
		TransactionDoc element = S.transactionTemplate.id(dto.getId());

		if (element != null) {
			return messagingService.response(dto.getId(), element);
		} else {
			return messagingService.response(dto.getId(), OperationStatus.NOT_FOUND);
		}
	}
	
	/**
	 * Creates a transaction based on the provided request and performs an optional additional execution for complementary processing.
	 * <p>
	 * This method validates the transaction creation request, retrieves the associated element (using the UDBN token),
	 * calculates tax amounts, and verifies that the sender has sufficient balance. It then stores the transaction in the ledger,
	 * linking it with the previous transaction ID using a dynamic sequence, and updates both sender's and recipient's balances.
	 * </p>
	 * 
	 * @param dto The {@link TransactionCreateDto} containing transaction details.
	 * @param additionalExecution A complementary operation to be executed as part of the transaction creation process.
	 * @return A {@link DataTransfer} object containing the result of the transaction creation.
	 */
	public DataTransfer createTransaction(TransactionCreateDto dto, Consumer<TransactionDoc> additionalExecution) {
		try {
			dto.validate();
		} catch (OperationException ex) {
			return messagingService.response(dto.getHash(), OperationStatus.ERROR, ex.getMessage());
		}

		ElementDoc element = S.elementTemplate.id(dto.getElement());
		if (element == null) {
			return messagingService.response(dto.getHash(), OperationStatus.ERROR, "Element not found");
		}

		String owner = Hasher.sha3Hex(Converter.base64OrHexToBytes(dto.getPublicKey()));

		TransactionDoc transaction = new TransactionDoc();
		transaction.setType(TransactionType.TRANSFER);
		transaction.setFrom(owner);
		transaction.setTo(dto.getTo());
		transaction.setVolume(new BigDecimal(dto.getVolume()).abs());
		transaction.setElement(dto.getElement());
		transaction.setNonce(dto.getNonce());
		transaction.setHash(dto.getHash());
		transaction.setPublicKey(dto.getPublicKey());
		transaction.setSignature(dto.getSignature());

		transaction.setTimestamp(System.currentTimeMillis());
		transaction.setShard(Shard.calculateWeekShard(transaction.getTimestamp()));
		transaction.setLedger(getCurrentLedger(transaction.getShard()));
		transaction.setPrivacy(element.getPrivacy());
		transaction.setVersion(1);
		
		// Check if a transaction with the same hash already exists in the current ledger.
		if (S.transactionTemplate.exists(Filters.eq("hash", dto.getHash()), transaction.getLedger()))
			return messagingService.response(dto.getHash(), OperationStatus.EXISTS);

		calculateTax(transaction, element);
		
		additionalExecution.accept(transaction);

		String semaphoreFrom = S.balanceService.buildId(transaction.getFrom(), transaction.getElement());
		String semaphoreTo = S.balanceService.buildId(transaction.getTo(), transaction.getElement());

		DataTransfer transportExecution = S.semaphoreService.execute(semaphoreFrom, _ -> {
			BalanceDoc balanceFrom = S.balanceService.get(transaction.getFrom(), transaction.getElement());

			if (balanceFrom.getAmount().compareTo(transaction.getTotal()) < 0) {
				return messagingService.response(dto.getHash(), OperationStatus.INSUFFICIENT_BALANCE, String.format("Your Need %s", transaction.getTotal()));
			}

			S.semaphoreService.execute(transaction.getElement(), _ -> {
				DynamicSequenceDoc dynamicSequence = S.dynamicSequenceService.get(transaction.getElement());
				transaction.setPreviousId(dynamicSequence.getData());
				transaction.setId(Hasher.sha3Hex(Strings.concatenate(transaction.getPreviousId(), transaction.getHash())));
				
				S.transactionTemplate.save(transaction, transaction.getLedger());
				S.dynamicSequenceService.set(transaction.getElement(), transaction.getId());
			});

			if (transaction.getId() == null) {
				return messagingService.response(dto.getHash(), OperationStatus.ERROR, "Transaction not confirmed.");
			}

			S.balanceService.set(balanceFrom, balanceFrom.getAmount().subtract(transaction.getVolume()));
			return messagingService.response(dto.getHash(), transaction); 
		});

		if (transportExecution.getStatus() != OperationStatus.OK) {
			return transportExecution;
		}
		
		S.semaphoreService.execute(semaphoreTo, _ -> {
			BalanceDoc balanceTo = S.balanceService.get(transaction.getTo(), transaction.getElement());
			S.balanceService.set(balanceTo, balanceTo.getAmount().add(transaction.getVolume()));
		});

		Long blockNumber = S.blockService.addTransactionToBlock(transaction);

		Bson update = Updates.combine(Updates.set("confirmed", true), Updates.set("block", blockNumber));
		S.transactionTemplate.update(Filters.eq("_id", transaction.getId()), update, transaction.getLedger());

		//captureTax(transaction, element);
		
		TransactionDoc result = S.transactionTemplate.id(transaction.getId(), transaction.getLedger());
		return messagingService.response(dto.getHash(), result);
	}
	
//	private DataTransfer captureTax(TransactionDoc transaction, ElementDoc element) {
//		TransactionDoc transactionTax = new TransactionDoc();
//		transactionTax.setType(TransactionType.CAPTURE);
//		transactionTax.setFrom(transaction.getFrom());
//		transactionTax.setTo(S.norshCoin.getOwner());
//		transactionTax.setVolume(transaction.getTotalTax());
//		transactionTax.setElement(transaction.getElement());
//		transactionTax.setNonce(0l);
//		transactionTax.setHash(transaction.getHash());
//		transactionTax.setTimestamp(System.currentTimeMillis());
//		transactionTax.setShard(Shard.calculateWeekShard(transactionTax.getTimestamp()));
//		transactionTax.setLedger(getCurrentLedger(transactionTax.getShard()));
//		transactionTax.setLink(transaction.getId());
//
//		transactionTax.setPrivacy(element.getPrivacy());
//		transactionTax.setVersion(1);
//		
//		calculateTax(transactionTax, element);
//		
//		String semaphoreFrom = S.balanceService.buildId(transactionTax.getFrom(), transactionTax.getElement());
//		String semaphoreTo = S.balanceService.buildId(transactionTax.getTo(), transactionTax.getElement());
//
//		DataTransfer transportExecution = S.semaphoreService.execute(semaphoreFrom, _ -> {
//			BalanceDoc balanceFrom = S.balanceService.get(transactionTax.getFrom(), transactionTax.getElement());
//
//			S.semaphoreService.execute(transactionTax.getElement(), _ -> {
//				DynamicSequenceDoc dynamicSequence = dynamicSequenceService.get(transactionTax.getElement());
//				transactionTax.setPreviousId(dynamicSequence.getData());
//				transactionTax.setId(Hasher.sha3Hex(Strings.concatenate(transactionTax.getPreviousId(), transactionTax.getHash())));
//
//				S.transactionTemplate.save(transactionTax, transactionTax.getLedger());
//				S.dynamicSequenceService.set(transactionTax.getElement(), transactionTax.getId());
//			});
//
//			if (transactionTax.getId() == null) {
//				return messagingService.response(transaction.getHash(), OperationStatus.ERROR, "Transaction not confirmed.");
//			}
//
//			S.balanceService.set(balanceFrom, balanceFrom.getAmount().subtract(transactionTax.getVolume()));
//
//			return messagingService.response(transaction.getHash(), transactionTax); 
//		});
//
//		if (transportExecution.getStatus() != OperationStatus.OK) {
//			return transportExecution;
//		}
//		
//		S.semaphoreService.execute(semaphoreTo, _ -> {
//			BalanceDoc balanceTo = S.balanceService.get(transactionTax.getTo(), transactionTax.getElement());
//			S.balanceService.set(balanceTo, balanceTo.getAmount().add(transactionTax.getVolume()));
//		});
//
//		Long blockNumber = S.blockService.addTransactionToBlock(transactionTax);
//
//		Update update = Update.update("confirmed", true).set("block", blockNumber);
//		S.transactionTemplate.update(Criteria.where("_id").is(transactionTax.getId()), update, TransactionDoc.class, transactionTax.getLedger());
//
//		TransactionDoc result = S.transactionTemplate.id(transactionTax.getId(), transactionTax.getLedger());
//		return messagingService.response(transaction.getHash(), result);
//	}

	/**
	 * Overloaded method to create a transaction without any additional complementary execution.
	 * 
	 * @param dto The {@link TransactionCreateDto} containing transaction details.
	 * @return A {@link DataTransfer} object containing the result of the transaction creation.
	 */
	@Processable(method = RestMethod.POST)
	public DataTransfer createTransaction(TransactionCreateDto dto) {
		return createTransaction(dto, _ -> {});
	}

	/**
	 * Computes and returns the current ledger name for the transaction based on its shard.
	 * 
	 * @param transaction The {@link TransactionDoc} for which to determine the ledger.
	 * @return A {@link String} representing the ledger collection name.
	 */
	private String getCurrentLedger(Long shard) {
		return Strings.concatenateWithSymbol("_", NorshConstants.getLedgerCollectionPrefix(), shard);
	}

	/**
	 * Calculates tax amounts for the transaction based on the associated element's policy and fee settings.
	 * <p>
	 * The method computes:
	 * <ul>
	 *   <li>The element tax amount using the transaction tax from the element's policy.</li>
	 *   <li>The network tax amount based on the standard network fee policy.</li>
	 *   <li>The total tax as the sum of the element and network tax amounts.</li>
	 *   <li>The total transaction amount as the sum of the transaction volume and total tax.</li>
	 * </ul>
	 * </p>
	 * 
	 * @param transaction The {@link TransactionDoc} to update with calculated tax values.
	 * @param element The {@link ElementDoc} providing policy and configuration for tax calculation.
	 */
	private void calculateTax(TransactionDoc transaction, ElementDoc element) {
		if (transaction.getType() == TransactionType.CAPTURE || transaction.getType() == TransactionType.REWARD || transaction.getVolume().compareTo(BigDecimal.ZERO) == 0) {
			transaction.setElementTax(BigDecimal.ZERO);
		    transaction.setNetworkTax(BigDecimal.ZERO);
		    transaction.setTotalTax(BigDecimal.ZERO);
		    transaction.setTotal(transaction.getVolume());
		    return;
		}
		
	    BigDecimal PERCENTAGE_DIVISOR = BigDecimal.valueOf(100);

	    BigDecimal elementTax = (element.getPolicy() == null || element.getPolicy().getTransactionTax() == null) 
	            ? BigDecimal.ZERO 
	            : BigDecimal.valueOf(element.getPolicy().getTransactionTax()).divide(PERCENTAGE_DIVISOR, element.getDecimals(), RoundingMode.HALF_UP);

	    BigDecimal networkTax = FeePolicy.getNetworkTax().divide(PERCENTAGE_DIVISOR, element.getDecimals(), RoundingMode.HALF_UP);

	    // Calculate individual tax amounts
	    BigDecimal elementTaxAmount = transaction.getVolume().multiply(elementTax);
	    BigDecimal networkTaxAmount = transaction.getVolume().multiply(networkTax);

	    // Sum up the total tax
	    BigDecimal totalTax = elementTaxAmount.add(networkTaxAmount);

	    // Update the transaction with tax and total values
	    transaction.setElementTax(elementTaxAmount);
	    transaction.setNetworkTax(networkTaxAmount);
	    transaction.setTotalTax(totalTax);
	    transaction.setTotal(transaction.getVolume().add(totalTax));
	}
}
