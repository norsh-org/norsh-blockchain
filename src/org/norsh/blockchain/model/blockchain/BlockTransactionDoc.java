package org.norsh.blockchain.model.blockchain;
import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a transaction within a block in the Norsh blockchain.
 * <p>
 * Each transaction is uniquely identified and contains metadata about the associated ledger
 * and transaction fees. This class serves as the fundamental representation of a blockchain transaction.
 * </p>
 *
 * @license NCL-R
 * @author Danthur Lice
 * @since 01/2025
 * @version 1.0
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
@Getter
@Setter
public class BlockTransactionDoc {
	/**
     * Unique identifier for the transaction.
     */
    private String id;

    /**
     * Identifier of the ledger where the transaction is recorded.
     */
    private String ledger;
    
    
    private String element;

    /**
     * Fee for the transaction paid to the miner.
     */
    private BigDecimal tax;
    
    /**
     * Volume
     */
    private BigDecimal volume;
    
    private Boolean privacy;
}
