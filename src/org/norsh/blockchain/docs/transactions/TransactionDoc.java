package org.norsh.blockchain.docs.transactions;

import java.math.BigDecimal;
import java.util.Map;

import org.norsh.model.types.TransactionType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a transaction within a ledger in the Norsh blockchain.
 * <p>
 * Transactions are stored within the ledger and include all necessary metadata for validation, accounting,
 * and cryptographic integrity. The transaction ID is linked with the previous transaction's ID, forming a chain
 * of transactions within the ledger.
 * </p>
 * <p>
 * <strong>ID Calculation Example:</strong>
 * <pre>
 * DynamicSequenceDoc dynamicSequence = dynamicSequenceService.get(transaction.getUdbn());
 * transaction.setPreviousId(dynamicSequence.getData());
 * transaction.setId(Hasher.sha3Hex(Strings.concatenate(transaction.getPreviousId(), transaction.getHash(), transaction.getTimestamp())));
 * </pre>
 * </p>
 * 
 * @author Danthur
 * @since 1.0.0
 * @version 1.0.0
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
@Setter
@Getter
@Document("ledger_w")
public class TransactionDoc {
	/**
	 * Unique identifier for the transaction.
	 * <p>
	 * The transaction ID is linked with the previous transaction's ID, forming a chain within the ledger.
	 * It is computed by concatenating the previous transaction ID, the transaction's hash, and the timestamp,
	 * then applying a SHA-3 hash.
	 * </p>
	 */
	@Id
	private String id;

	/**
	 * Identifier of the previous transaction.
	 * <p>
	 * This field links the current transaction to its predecessor, creating a sequential chain of transactions within the ledger.
	 * </p>
	 */
	private String previousId;

	/**
	 * The owner (sender) associated with the transaction.
	 * <p>
	 * This field identifies the entity or user responsible for initiating the transaction.
	 * </p>
	 */
	private String from;
	
	/**
	 * The recipient of the transaction.
	 */
	private String to;
	
	/**
	 * Element associated with the transaction.
	 */
	private String element;
	
	/**
	 * The volume associated with the transaction.
	 */
	private BigDecimal volume;

	/**
	 * The nonce associated with the transaction.
	 * <p>
	 * A nonce (number used once) is a unique value used for cryptographic purposes, such as preventing replay
	 * attacks or ensuring the uniqueness of the transaction hash.
	 * </p>
	 */
	private Long nonce;

	/**
	 * The hash representing the transaction data.
	 * <p>
	 * This hash is a fundamental part of ensuring the transaction's integrity and is used as input for generating the transaction ID.
	 * </p>
	 */
	private String hash;

	/**
	 * The public key used to verify the transaction's signature.
	 * <p>
	 * This field contains the public key corresponding to the private key used to sign the transaction.
	 * It is used to validate the authenticity and integrity of the transaction.
	 * </p>
	 */
	private String publicKey;

	/**
	 * The cryptographic signature associated with the transaction.
	 * <p>
	 * This field contains the signature that validates the integrity and authenticity of the transaction.
	 * The signature is generated using the sender's private key and can be verified against the corresponding public key.
	 * </p>
	 */
	private String signature;

	/**
	 * An additional identifier for transaction categorization.
	 */
	private TransactionType type;

	/**
	 * Tax associated with the contract execution.
	 */
	private BigDecimal elementTax;

	/**
	 * Tax distributed across the network participants.
	 */
	private BigDecimal networkTax;

	/**
	 * Total tax amount for the transaction.
	 */
	private BigDecimal totalTax;

	/**
	 * Total volume transferred, including related calculations.
	 */
	private BigDecimal total;

	/**
	 * The timestamp when the transaction was created, represented in epoch milliseconds.
	 * <p>
	 * This field helps in tracking the creation time of the transaction and can be useful for auditing or chronological ordering.
	 * </p>
	 */
	private Long timestamp;
	
	/**
	 * Another transaction link.
	 */
	private String link;
	
	/**
	 * Transaction metadata.
	 */
	private Map<String, Object> metadata;
	
	/**
	 * Indicates whether the transaction is visible.
	 */
	private Boolean privacy;

	/**
	 * The shard number associated with the transaction.
	 */
	private Long shard;

	/**
	 * The ledger in which the transaction is recorded.
	 */
	private String ledger;

	/**
	 * The block within the ledger where the transaction resides.
	 */
	private Long block;
	
	/**
	 * The version of the transaction structure.
	 * <p>
	 * This field indicates the version of the transaction's data structure, allowing for future updates or schema changes
	 * while maintaining backward compatibility with previous versions.
	 * </p>
	 */
	private Integer version;
}
