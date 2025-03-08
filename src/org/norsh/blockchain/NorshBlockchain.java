package org.norsh.blockchain;

import java.util.Calendar;

import org.norsh.blockchain.services.BootstrapSetup;
import org.norsh.blockchain.v1.DataTransferApiV1;
import org.norsh.rest.HttpServer;
import org.norsh.util.Log;

/**
 * Main class for the Norsh Blockchain worker.
 * <p>
 * This application is responsible for handling blockchain operations asynchronously, processing incoming data from a
 * queue, and ensuring ledger consistency.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 * <li>Processes blockchain transactions from an incoming queue.</li>
 * <li>Ensures ledger synchronization and cryptographic validation.</li>
 * <li>Logs system metadata and operational parameters at startup.</li>
 * <li>Operates continuously as a standalone process.</li>
 * </ul>
 *
 * <h2>Startup Process:</h2>
 * <ol>
 * <li>Loads blockchain configurations.</li>
 * <li>Initializes queue listener and processing services.</li>
 * <li>Logs system metadata and operational parameters.</li>
 * </ol>
 *
 * @author Danthur Lice
 * @license NCL-139
 * @since 1.0.0
 * @version 1.0.0
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
public class NorshBlockchain {
	/**
     * Entry point for the Norsh Blockchain worker.
     * <p>
     * This method initializes the application, setting up necessary configurations and starting the queue processing
     * service.
     * </p>
     */
	public static void main(String[] args) {
//		BlockchainConfig.initializeDefaultLocalization();
//		SpringApplication app = new SpringApplication(NorshBlockchain.class);
//		app.setBannerMode(Banner.Mode.OFF);
//		app.setDefaultProperties(BlockchainConfig.getInstance().getSpringProperties());
//		app.run(args);
		
		Log log = S.log;
		log.system("Norsh Blockchain ○ ●●");
		log.system("Developed by " + String.join(", ", "Danthur Lice") + " and contributors.");
		log.system(String.format("Copyright © 2024-%s Norsh. All rights reserved", Calendar.getInstance().get(Calendar.YEAR)));
		log.system(String.format("Server address: %s:%s", S.config.getSpringProperties().get("server.host"), S.config.getSpringProperties().get("server.port")));

		new BootstrapSetup().start();

		HttpServer httpServer = new HttpServer();
		//httpServer.setExceptionHandler(new ApiThrowableHandler());
		httpServer.addEndpoint(DataTransferApiV1.class);
		httpServer.start(4545, false);

		log.system("Server started.");
		log.breakLine();
		
	}
}