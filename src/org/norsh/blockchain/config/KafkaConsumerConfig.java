package org.norsh.blockchain.config;

import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.norsh.config.KafkaConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kafka consumer configuration for subscribing to blockchain-related topics.
 * <p>
 * This configuration initializes a {@link KafkaConsumer} that listens to messages 
 * from the blockchain transaction system, ensuring efficient message processing.
 * </p>
 *
 * <h2>Consumer Settings:</h2>
 * <ul>
 *   <li>Reads from the earliest available offset to avoid data loss.</li>
 *   <li>Uses {@link StringDeserializer} for both key and value.</li>
 *   <li>Clears sensitive Kafka configurations after initialization.</li>
 * </ul>
 *
 * <h2>Example Usage:</h2>
 * <pre>
 * KafkaConsumer<String, String> consumer = kafkaConsumer();
 * </pre>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
@Configuration
public class KafkaConsumerConfig {

    /**
     * Initializes a Kafka consumer for blockchain message processing.
     * <p>
     * The consumer subscribes to blockchain-related topics and reads messages
     * from the earliest available offset to avoid missing historical transactions.
     * </p>
     *
     * @return a {@link KafkaConsumer} instance ready for message consumption.
     */
    @Bean
    public KafkaConsumer<String, String> kafkaConsumer() {
        KafkaConfig kafkaConfig = BlockchainConfig.getInstance().getKafkaConfig();
        
        if (kafkaConfig == null) {
            throw new IllegalStateException("Kafka configuration is missing. Ensure it is properly set in BlockchainConfig.");
        }

        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConfig.getGroupId());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // Ensures no messages are skipped
        //properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        // Subscribe only if topics are available
        if (kafkaConfig.getTopic() != null) {
            consumer.subscribe(List.of(kafkaConfig.getTopic()));
        }

        // Clears Kafka configuration from memory to enhance security
        BlockchainConfig.getInstance().clearKafkaConfig();

        return consumer;
    }
}
