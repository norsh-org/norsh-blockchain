package org.norsh.blockchain.config;

import org.norsh.config.Config;
import org.norsh.config.LogConfig;

/**
 * Configuration loader and manager for the blockchain module.
 * <p>
 * This class is responsible for loading and providing access to blockchain-specific 
 * configuration settings, such as logging configurations and server properties.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Automatically loads blockchain configurations from predefined locations.</li>
 *   <li>Provides access to logging and server configuration settings.</li>
 *   <li>Ensures consistent configuration management across blockchain services.</li>
 * </ul>
 *
 * <h2>Configuration File Locations:</h2>
 * <ul>
 *   <li>Default: {@code /etc/norsh/blockchain.json}</li>
 *   <li>Environment Variable: {@code NORSH_BLOCKCHAIN_CONFIG}</li>
 * </ul>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 * @see LogConfig
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
public class BlockchainConfig extends Config {
    private static final BlockchainConfig instance = new BlockchainConfig();

    static {
        instance.load("NORSH_BLOCKCHAIN_CONFIG", "/etc/norsh/blockchain.json");
    }
    
    public static BlockchainConfig getInstance() {
		return instance;
	}
}
