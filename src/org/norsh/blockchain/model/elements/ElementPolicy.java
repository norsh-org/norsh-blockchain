package org.norsh.blockchain.model.elements;

import lombok.Getter;
import lombok.Setter;

/**
 * Defines the policy configuration for a Smart Element.
 * <p>
 * This class establishes governance rules, permissions, and transactional constraints 
 * that dictate the behavior of a Smart Element. It includes controls for minting, burning, 
 * pausing transactions, transaction taxation, freeze durations, and execution logic.
 * </p>
 *
 * <h2>Policy Features:</h2>
 * <ul>
 *   <li>Controls whether new tokens can be minted ({@link #canMint}).</li>
 *   <li>Determines whether tokens can be burned ({@link #canBurn}).</li>
 *   <li>Specifies if transactions can be paused ({@link #canPause}).</li>
 *   <li>Applies a global tax percentage to transactions ({@link #transactionTax}).</li>
 *   <li>Enforces a minimum freeze duration before asset transfers ({@link #freezeDuration}).</li>
 *   <li>Allows the execution of a custom script for policy enforcement ({@link #script}).</li>
 * </ul>
 *
 * <h2>Validation Rules:</h2>
 * <ul>
 *   <li>{@link #transactionTax} must be between 0 and 100 (percentage-based).</li>
 *   <li>{@link #freezeDuration} must be greater than or equal to 0.</li>
 *   <li>{@link #script} must be in Base64 format if provided.</li>
 * </ul>
 *
 * <h2>Example Usage:</h2>
 * <pre>
 * ElementPolicy policy = new ElementPolicy();
 * policy.setCanMint(true);
 * policy.setCanBurn(false);
 * policy.setCanPause(true);
 * policy.setTransactionTax(2.5);
 * policy.setFreezeDuration(24);
 * policy.setScript("BASE64_ENCODED_SCRIPT");
 * </pre>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
@Getter
@Setter
public class ElementPolicy {

    /**
     * Determines whether the Smart Element has minting privileges.
     * <p>
     * If {@code true}, new tokens can be created for this element.
     * </p>
     */
    private Boolean canMint;

    /**
     * Specifies whether the Smart Element allows burning tokens.
     * <p>
     * If {@code true}, tokens associated with this element can be permanently destroyed.
     * </p>
     */
    private Boolean canBurn;

    /**
     * Controls whether transactions involving this Smart Element can be paused.
     * <p>
     * If {@code true}, transactions can be temporarily halted as part of governance policies.
     * </p>
     */
    private Boolean canPause;

    /**
     * The global transaction tax percentage applied to transfers involving this Smart Element.
     * <p>
     * This value represents the percentage of the transaction amount that will be collected 
     * as a fee. The tax must be between 0 and 100.
     * </p>
     */
    private Double transactionTax;

    /**
     * The minimum duration (in hours) before an asset can be transferred after being received.
     * <p>
     * This policy enforces time-based restrictions on asset transfers to prevent rapid 
     * movement of funds, ensuring compliance with predefined holding periods.
     * </p>
     */
    private Integer freezeDuration; 

    /**
     * The execution script associated with the Smart Element.
     * <p>
     * This script defines additional transaction rules, automation logic, 
     * or contract execution conditions. If provided, it must be encoded in Base64 format.
     * </p>
     */
    private String script;
}
