package org.norsh.blockchain.services.queue;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.norsh.blockchain.config.BlockchainConfig;
import org.norsh.util.Log;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer Service for processing blockchain-related messages.
 * <p>
 * This service listens for Kafka messages and processes them dynamically based on the distributed DTO structure.
 * </p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
@Component
public class QueueConsumerService {
	private volatile boolean running = true;
	private final Log log;
	private final KafkaConsumer<String, String> consumer;
	private final ExecutorService executorService;
	private final DispatcherService dispatcherService;

	/**
	 * Initializes the Kafka Consumer using settings from `blockchain.json`.
	 */
	public QueueConsumerService(KafkaConsumer<String, String> consumer, DispatcherService dispatcherService, Log log) {
		this.consumer = consumer;
		this.dispatcherService = dispatcherService;
		this.log = log;
		this.executorService = Executors.newFixedThreadPool(BlockchainConfig.getInstance().getDefaultsConfig().getQueueConsumerThreadPool());
	}

	/**
	 * Starts the Kafka consumer loop.
	 */
	public void startConsumer() {
		new Thread(() -> {
			log.info("Kafka Consumer Service started");

			while (running) {
				try {
					ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

					for (ConsumerRecord<String, String> record : records) {
						executorService.submit(() -> {
							dispatcherService.dispatch(record.value());
						});
					}
				} catch (Exception e) {
					if (!running) {
						log.warning("Kafka consumer shutting down...");
					} else {
						log.error("Kafka consumer encountered an error", e);
					}
				}
			}

			log.info("Kafka Consumer Service stopped.");
		}).start();
	}

	/**
	 * Gracefully shuts down the Kafka consumer and executor service.
	 */
	public void shutdown() {
		if (!running)
			return;

		log.system("Shutting down Kafka Consumer Service...");

		running = false;

		try {
			consumer.wakeup();
			consumer.close();
		} catch (Exception _) {
		}

		executorService.shutdown();

		try {
			if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
				log.warning("Forcing shutdown of workers...");
				executorService.shutdownNow();
			}
		} catch (Exception _) {
			executorService.shutdownNow();
		}

		log.system("Kafka Consumer Service has been shut down.");
	}
}
