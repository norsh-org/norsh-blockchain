package org.norsh.blockchain;

import java.util.Calendar;

import org.norsh.blockchain.config.BlockchainConfig;
import org.norsh.blockchain.services.BootstrapSetup;
import org.norsh.blockchain.services.queue.QueueConsumerService;
import org.norsh.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

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
@SpringBootApplication
@ComponentScan("org.norsh.blockchain")
@EnableAutoConfiguration(exclude = {MongoAutoConfiguration.class} )
public class NorshBlockchain {
	@Autowired
	private BootstrapSetup bootstrapService;
	
	@Autowired
	private Log log;

	@Autowired
	private QueueConsumerService consumerService;
	
	/**
     * Entry point for the Norsh Blockchain worker.
     * <p>
     * This method initializes the application, setting up necessary configurations and starting the queue processing
     * service.
     * </p>
     */
	public static void main(String[] args) {
		
		
		BlockchainConfig.initializeDefaultLocalization();
		SpringApplication app = new SpringApplication(NorshBlockchain.class);
		app.setBannerMode(Banner.Mode.OFF);
		app.setDefaultProperties(BlockchainConfig.getInstance().getSpringProperties());
		app.run(args);
	}

	/**
     * Logs system configuration details during startup.
     * <p>
     * This method runs automatically after the application initializes and records essential metadata, including system
     * information and author credits.
     * </p>
     */
	@PostConstruct
	private void bootstrap() {
		log.system("Norsh Blockchain");
		log.system("Developed by " + String.join(", ", "Danthur Lice") + " and contributors.");
		log.system(String.format("Copyright © 2024-%s Norsh. All rights reserved", Calendar.getInstance().get(Calendar.YEAR)));
		log.system("Application started.");
		log.breakLine();

		bootstrapService.start();
		
		consumerService.startConsumer();
	}

	 /**
     * Ensures all services are properly shut down before termination.
     */
	@PreDestroy
	private void shutdown() {
		log.system("Starting shutdown process for Norsh Blockchain...");
		consumerService.shutdown();
		//shutdown mongo
		//shutdown redis
	}
}