package org.norsh.blockchain.services.database;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

/**
 * Generic service for interacting with MongoDB.
 * <p>
 * This service provides common database operations such as saving, retrieving, updating, and deleting documents in
 * MongoDB.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 * <li>Provides generic CRUD operations.</li>
 * <li>Handles document retrieval by ID.</li>
 * <li>Supports listing, updating, and deleting documents using criteria.</li>
 * <li>Seamless integration with MongoDB using Spring Data.</li>
 * </ul>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 */

@Service
public class MongoRead extends AbstractMongoService {
	public MongoRead(@Qualifier("mongoTemplateRead") MongoTemplate mongoTemplate) {
		super(mongoTemplate);
	}
}