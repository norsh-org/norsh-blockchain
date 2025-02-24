package org.norsh.blockchain.components;

/**
 * Defines global constants used throughout the Norsh application.
 * <p>
 * This class provides centralized access to key application-wide constants,
 * ensuring consistency and maintainability while preventing runtime modifications
 * via reflection-based attacks.
 * </p>
 *
 * <h2>Security Hardening:</h2>
 * <ul>
 * <li>All constants are now accessed via **methods**, preventing unauthorized modification.</li>
 * <li>No direct access to fields ensures that values **cannot be altered via reflection (`setAccessible(true)`)**.</li>
 * <li>Singleton-style **static final class** prevents instantiation and subclassing.</li>
 * </ul>
 *
 * <h2>Example Usage:</h2>
 * <pre>
 * String prefix = NorshConstants.getProxySuffix();
 * String wallet = NorshConstants.getNorshWallet();
 * </pre>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 */
public final class NorshConstants {
    private NorshConstants() {
        // Prevent instantiation
    }

    /**
     * Returns the prefix used for ledger collections.
     * @return The ledger collection prefix.
     */
    public static String getLedgerCollectionPrefix() {
        return "ledger";
    }

    /**
     * Returns the sequence key used for blockchain block numbering.
     * @return The blockchain block number sequence key.
     */
    public static String getSequenceBlockNumber() {
        return "blockchain-block-number";
    }
    
    public static String getSequenceBlockId() {
        return "blockchain-block-id";
    }

    /**
     * Returns the block time interval in minutes.
     * @return The block time interval.
     */
    public static int getBlockWindowIntervalMin() {
        return 6;
    }

    /**
     * Returns the semaphore identifier for blockchain operations.
     * @return The blockchain semaphore identifier.
     */
    public static String getSemaphoreBlockchain() {
        return "blockchain";
    }

    /**
     * Returns the semaphore identifier for Smart Elements.
     * @return The elements semaphore identifier.
     */
    public static String getTagElements() {
        return "elements";
    }

    /**
     * Returns the proxy suffix used in Smart Elements.
     * @return The proxy suffix.
     */
    public static String getProxySuffix() {
        return "-P";
    }
}
