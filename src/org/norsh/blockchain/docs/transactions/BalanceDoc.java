package org.norsh.blockchain.docs.transactions;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a balance entry in the Norsh ledger system.
 * <p>
 * This document tracks the balance of a specific token for a given owner, enabling efficient management and querying of
 * token holdings.
 * </p>
 * 
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
@Getter
@Setter
@Document("balances")
public class BalanceDoc {
	/**
	 * Unique identifier for the balance document.
	 * <p>
	 * This ID is derived as a hash of the concatenation of {@code owner} and {@code token}, ensuring uniqueness for each
	 * combination of owner and token.
	 * </p>
	 */
	@Id
	private String id;

	/**
	 * The owner associated with the balance.
	 * <p>
	 * This field identifies the account or entity that owns the token balance. It is indexed sparsely to optimize queries
	 * while allowing null values.
	 * </p>
	 */
	@Indexed(sparse = true)
	private String owner;

	/**
	 * The token associated with this balance entry.
	 * <p>
	 * Represents the type of token (e.g., NSH, or a specific asset) being tracked in this balance. Indexed for efficient
	 * lookup in queries.
	 * </p>
	 */
	@Indexed
	private String token;

	/**
	 * The amount of the token held by the owner.
	 * <p>
	 * This field records the current balance of the specified token for the owner.
	 * </p>
	 */
	private BigDecimal amount;
}
