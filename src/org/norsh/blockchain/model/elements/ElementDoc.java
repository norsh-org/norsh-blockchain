package org.norsh.blockchain.model.elements;

import java.util.Map;

import org.norsh.blockchain.model.ADoc;
import org.norsh.model.types.ElementType;
import org.norsh.model.types.Networks;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents an Element within the Norsh blockchain system.
 * <p>
 * This document defines the structure of a Smart Element, including its metadata, policies, 
 * and governance settings. Elements can represent tokens, assets, or proxy elements 
 * that monitor external networks.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Immutable identifier for tracking the element across transactions.</li>
 *   <li>Supports various types such as native tokens, smart elements, and proxy assets.</li>
 *   <li>Includes governance policies to manage minting, burning, and tax rules.</li>
 *   <li>Stores cryptographic signatures for verification and integrity.</li>
 * </ul>
 *
 * @author Danthur Lice
 * @since 1.0.0
 * @version 1.0.0
 * @see ElementType
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class ElementDoc extends ADoc {
    /**
     * Identifier of the previous version of this element.
     * <p>
     * Used for tracking modifications and maintaining versioning history.
     * </p>
     */
    private String previousId;

    /**
     * Owner or creator of the element.
     * <p>
     * This field represents the entity that initially registered the Smart Element.
     * </p>
     */
    private String owner;

    /**
     * Symbol representing the element (e.g., "NSH").
     * <p>
     * This is used for referencing the element in transactions.
     * </p>
     */
    private String symbol;

    /**
     * Type of the Smart Element.
     * <p>
     * Defines whether the element is a native token, a proxy, or another type. 
     * See {@link ElementType} for valid types.
     * </p>
     */
    private ElementType type;

    /**
     * Number of decimal places supported by the element.
     * <p>
     * Defines the level of precision for asset transactions (e.g., 6 for NSH).
     * </p>
     */
    private Integer decimals;
    
    private Long initialSupply;

    private String tfo;
    /**
     * Unique hash representing this element's data.
     * <p>
     * This hash ensures data integrity and is used for verification purposes.
     * </p>
     */
    private String hash;

    /**
     * Timestamp of the element's creation, in epoch milliseconds.
     */
    private Long timestamp;

    /**
     * Indicates whether transactions related to this element are public or private.
     * <p>
     * If {@code true}, transactions are visible within the blockchain explorer.
     * </p>
     */
    private Boolean privacy;

    /**
     * Monitored networks for proxy elements.
     * <p>
     * Maps network identifiers to their respective monitored addresses.
     * </p>
     */
    private Map<String, Networks> monitoredNetworks;

    /**
     * Governance policies defining the element’s behavior.
     * <p>
     * Includes settings for minting, burning, taxation, and execution scripts.
     * </p>
     */
    private ElementPolicy policy;

    /**
     * Cryptographic signature verifying the authenticity of this element.
     * <p>
     * The signature is generated using the owner's private key and follows the ECDSA standard.
     * </p>
     */
    private String signature;

    /**
     * Public key associated with the element's signature.
     * <p>
     * Used to verify the authenticity of transactions and policy changes.
     * </p>
     */
    private String publicKey;

    /**
     * Additional metadata related to the element.
     * <p>
     * This field allows storing custom attributes, configurations, or extra data related to the Smart Element.
     * </p>
     */
    private Map<String, Object> metadata;

    /**
     * Version number of this element.
     * <p>
     * Helps maintain backward compatibility and supports future upgrades.
     * </p>
     */
    private Integer version;

    /**
     * Current operational status of the element.
     * <p>
     * The status can be ACTIVE, PENDING, or other states defined in {@link ElementStatus}.
     * </p>
     */
    private ElementStatus status;
}
