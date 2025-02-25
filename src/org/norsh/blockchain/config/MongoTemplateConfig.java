package org.norsh.blockchain.config;

import org.norsh.config.MongoConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoClients;

/**
 * Configuration class for MongoDB connections.
 * <p>
 * This class initializes multiple {@link MongoTemplate} beans for different database operations, ensuring separation of
 * concerns between core transactions and read-intensive queries.
 * </p>
 *
 * <h2>MongoDB Configurations:</h2>
 * <ul>
 *   <li>{@link #mongoMain()} - Handles blockchain transaction data.</li>
 *   <li>{@link #mongoRead()} - Optimized for read-intensive operations.</li>
 * </ul>
 *
 * <h2>Configuration Sources:</h2>
 * <p>
 * The connection details are fetched from {@link BlockchainConfig}, providing different configurations for:
 * </p>
 * <ul>
 *   <li>{@code mainConfig} - Used for main blockchain transactions.</li>
 *   <li>{@code readConfig} - Used for optimized read queries.</li>
 * </ul>
 *
 * <h2>Read Preference Strategy:</h2>
 * <ul>
 *   <li>{@code mongoMain()} uses the primary node for consistency in transactions.</li>
 *   <li>{@code mongoRead()} prefers secondary nodes for distributed read operations, improving performance.</li>
 * </ul>
 *
 * <h2>Automatic Cleanup:</h2>
 * <ul>
 *   <li>Once both connections are initialized, the MongoDB configuration is removed from memory using {@link BlockchainConfig#clearMongoConfig(String)}.</li>
 *   <li>Ensures that sensitive configuration details are not stored in memory after initialization.</li>
 * </ul>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
@Configuration
public class MongoTemplateConfig {
    private volatile boolean mongoMainLoaded = false, mongoReadLoaded = false;

    /** Configuration for main blockchain operations. */
    private final MongoConfig mainConfig = BlockchainConfig.getInstance().getMongoConfig("main");

    /**
     * Creates a {@link MongoTemplate} bean for transaction-related database operations.
     * <p>
     * This template connects to the primary node, ensuring consistency for blockchain transactions.
     * </p>
     *
     * @return a configured {@link MongoTemplate} for blockchain transactions.
     */
    @Bean("mongoTemplateMain")
    public MongoTemplate mongoMain() {
    	if (mainConfig == null) {
            throw new IllegalStateException("Mongo configuration is missing. Ensure it is properly set in BlockchainConfig.");
        }
    	
        MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(mainConfig.getConnectionString()))
            //.readPreference(ReadPreference.primary()) // Prioritize primary node for read
            .build();

        MongoTemplate mongoTemplate = new MongoTemplate(MongoClients.create(settings), mainConfig.getDatabase());
        ((MappingMongoConverter) mongoTemplate.getConverter()).setTypeMapper(new DefaultMongoTypeMapper(null));
        
        synchronized (this) {
            mongoMainLoaded = true;
            cleanupConfig();
        }

        return mongoTemplate;
    }

    /**
     * Creates a {@link MongoTemplate} bean optimized for read-intensive operations.
     * <p>
     * This template uses a read preference of {@link ReadPreference#secondaryPreferred()}, meaning it will prioritize
     * secondary nodes for queries, improving scalability and performance.
     * </p>
     *
     * @return a configured {@link MongoTemplate} for optimized read operations.
     */
    @Bean("mongoTemplateRead")
    public MongoTemplate mongoRead() {
    	if (mainConfig == null) {
            throw new IllegalStateException("Mongo configuration is missing. Ensure it is properly set in BlockchainConfig.");
        }
    	
        MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(mainConfig.getConnectionString()))
            //.readPreference(ReadPreference.secondaryPreferred()) // Prioritize secondary nodes for reads
            .build();

        MongoTemplate mongoTemplate = new MongoTemplate(MongoClients.create(settings), mainConfig.getDatabase());
        ((MappingMongoConverter) mongoTemplate.getConverter()).setTypeMapper(new DefaultMongoTypeMapper(null));
        
        synchronized (this) {
            mongoReadLoaded = true;
            cleanupConfig();
        }

        return mongoTemplate;
    }

    /**
     * Removes MongoDB configuration from memory after both main and read connections have been initialized.
     * <p>
     * This ensures that sensitive database connection details are not retained unnecessarily in memory.
     * </p>
     */
    private void cleanupConfig() {
        if (mongoMainLoaded && mongoReadLoaded) {
            BlockchainConfig.getInstance().clearMongoConfig("main");
        }
    }
}
