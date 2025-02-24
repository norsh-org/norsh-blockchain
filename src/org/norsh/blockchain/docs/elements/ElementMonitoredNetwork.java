package org.norsh.blockchain.docs.elements;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a monitored network within the system.
 * <p>
 * This class is used to define the attributes of a network being monitored
 * and the list of associated addresses for a smart element.
 * </p>
 *
 * <h2>Fields:</h2>
 * <ul>
 *   <li>{@link #network}: The name of the network (e.g., Ethereum, Solana).</li>
 *   <li>{@link #addresses}: A list of addresses being monitored within the network.</li>
 * </ul>
 *
 * @license NCL-R
 * @author Danthur Lice
 * @since 01/2025
 * @version 1.0
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
@Getter
@Setter
public class ElementMonitoredNetwork {
    /** The name of the network (e.g., ETH, SOL). */
    private String network;

    /** A list of addresses being monitored within the network. */
    private List<String> addresses;
}
