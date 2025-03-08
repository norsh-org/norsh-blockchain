package org.norsh.blockchain.model.elements;

/**
 * Enum representing the status of a Smart Element in the system.
 * <p>
 * The status determines the operational state of a smart element.
 * </p>
 *
 * <h2>Status Values:</h2>
 * <ul>
 *   <li>{@link #PENDING}: The Smart Element is pending approval or activation.</li>
 *   <li>{@link #ENABLED}: The Smart Element is active and operational.</li>
 *   <li>{@link #DISABLED}: The Smart Element is inactive or deactivated.</li>
 * </ul>
 *
 * @license NCL-R
 * @since 01/2025
 * @version 1.0
 * @author Danthur
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
public enum ElementStatus {
    /** The Smart Element is pending approval or activation. */
    PENDING,

    /** The Smart Element is active and operational. */
    ENABLED,

    /** The Smart Element is inactive or deactivated. */
    DISABLED
}
