package org.norsh.blockchain.config;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.norsh.blockchain.S;
import org.norsh.config.MongoConfig;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * Singleton configuration for MongoDB connection.
 * Ensures a single instance of MongoDatabase is used across the application.
 *
 * @since 1.0.0
 * @version 1.0.1
 * @author Danthur Lice
 */
public class MongoConnectionConfig {
	private MongoClient mongoClient = null;
	private MongoDatabase mongoDatabase = null;

	/**
	 * Returns a singleton instance of MongoDatabase.
	 * If the instance is not initialized, it creates a new one.
	 *
	 * @return MongoDatabase instance.
	 */
	public synchronized MongoDatabase getDatabase() {
		MongoConfig mainConfig = S.config.getMongoConfig("main");

		if (mongoDatabase != null) {
			return mongoDatabase;
		}

		CodecRegistry pojoCodecRegistry = CodecRegistries.fromProviders(
				PojoCodecProvider.builder().automatic(true).build()
				);

		CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
				MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry
				);

		mongoClient = MongoClients.create(mainConfig.getConnectionString());
		mongoDatabase = mongoClient.getDatabase(mainConfig.getDatabase()).withCodecRegistry(codecRegistry);

		cleanupConfig();
		return mongoDatabase;
	}

	/**
	 * Closes the MongoDB connection.
	 * This method should be called when the application shuts down.
	 */
	public synchronized void close() {
		if (mongoClient != null) {
			mongoClient.close();
			mongoClient = null;
			mongoDatabase = null;
		}
	}

	/**
	 * Cleans up the MongoDB configuration from BlockchainConfig.
	 * Ensures that after initialization, unnecessary configurations are removed.
	 */
	private void cleanupConfig() {
		if (mongoDatabase != null) {
			S.config.clearMongoConfig("main");
		}
	}
}
