package org.norsh.blockchain.services.database;

import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

/**
 * Abstract service for interacting with MongoDB.
 * <p>
 * This service provides common database operations such as saving, retrieving, updating, and deleting documents
 * in MongoDB.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Provides generic CRUD operations.</li>
 *   <li>Handles document retrieval by ID.</li>
 *   <li>Supports listing, updating, and deleting documents using criteria.</li>
 *   <li>Seamless integration with MongoDB using Spring Data.</li>
 * </ul>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 */
public abstract class AbstractMongoService {
	private final MongoTemplate mongoTemplate;

	public AbstractMongoService(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public MongoTemplate getMongoTemplate() {
		return mongoTemplate;
	}
	
	/**
	 * Saves a document in MongoDB.
	 *
	 * @param document   The document to be saved.
	 * @param collection The collection name where the document will be stored.
	 * @param <T>        The type of the document.
	 * @return The saved document.
	 */
	public <T> T save(T document, String collection) {
		return mongoTemplate.save(document, collection);
	}

	/**
	 * Saves a document in MongoDB using the default collection.
	 *
	 * @param document The document to be saved.
	 * @param <T>      The type of the document.
	 * @return The saved document.
	 */
	public <T> T save(T document) {
		return mongoTemplate.save(document);
	}

	/**
	 * Retrieves a document by its ID.
	 *
	 * @param id         The unique identifier of the document.
	 * @param type       The class type of the document.
	 * @param collection The collection name where the document is stored.
	 * @param <T>        The type of the document.
	 * @return The retrieved document, or {@code null} if not found.
	 */
	public <T> T id(Object id, Class<T> type, String collection) {
		return mongoTemplate.findById(id, type, collection);
	}

	/**
	 * Retrieves a document by its ID from the default collection.
	 *
	 * @param id   The unique identifier of the document.
	 * @param type The class type of the document.
	 * @param <T>  The type of the document.
	 * @return The retrieved document, or {@code null} if not found.
	 */
	public <T> T id(Object id, Class<T> type) {
		return mongoTemplate.findById(id, type);
	}
	
	/**
	 * Retrieves a document by its ID.
	 *
	 * @param id         The unique identifier of the document.
	 * @param type       The class type of the document.
	 * @param collection The collection name where the document is stored.
	 * @param <T>        The type of the document.
	 * @return The retrieved document, or {@code null} if not found.
	 */
	public <T> T get(Criteria criteria, Class<T> type, String collection) {
		return mongoTemplate.findOne(Query.query(criteria), type, collection);
	}

	/**
	 * Retrieves a document by its ID from the default collection.
	 *
	 * @param id   The unique identifier of the document.
	 * @param type The class type of the document.
	 * @param <T>  The type of the document.
	 * @return The retrieved document, or {@code null} if not found.
	 */
	public <T> T get(Criteria criteria, Class<T> type) {
		return mongoTemplate.findOne(Query.query(criteria), type);
	}

	/**
	 * Checks if a document exists in the collection based on criteria.
	 *
	 * @param criteria   The search criteria.
	 * @param type       The class type of the document.
	 * @param collection The collection name where the document is stored.
	 * @return {@code true} if the document exists, otherwise {@code false}.
	 */
	public boolean exists(Criteria criteria, Class<?> type, String collection) {
		return mongoTemplate.exists(Query.query(criteria), type, collection);
	}

	/**
	 * Checks if a document exists in the default collection based on criteria.
	 *
	 * @param criteria The search criteria.
	 * @param type     The class type of the document.
	 * @return {@code true} if the document exists, otherwise {@code false}.
	 */
	public boolean exists(Criteria criteria, Class<?> type) {
		return mongoTemplate.exists(Query.query(criteria), type);
	}

	/**
	 * Retrieves a list of documents based on a query.
	 *
	 * @param query      The query criteria.
	 * @param type       The class type of the documents.
	 * @param collection The collection name where the documents are stored.
	 * @param <T>        The type of the documents.
	 * @return A list of matching documents.
	 */
	public <T> List<T> list(Query query, Class<T> type, String collection) {
		return mongoTemplate.find(query, type, collection);
	}

	/**
	 * Retrieves a list of documents based on a query from the default collection.
	 *
	 * @param query The query criteria.
	 * @param type  The class type of the documents.
	 * @param <T>   The type of the documents.
	 * @return A list of matching documents.
	 */
	public <T> List<T> list(Query query, Class<T> type) {
		return mongoTemplate.find(query, type);
	}

	/**
	 * Retrieves a list of documents based on criteria.
	 *
	 * @param criteria   The search criteria.
	 * @param type       The class type of the documents.
	 * @param collection The collection name where the documents are stored.
	 * @param <T>        The type of the documents.
	 * @return A list of matching documents.
	 */
	public <T> List<T> list(Criteria criteria, Class<T> type, String collection) {
		return list(Query.query(criteria), type, collection);
	}

	/**
	 * Retrieves a list of documents based on criteria from the default collection.
	 *
	 * @param criteria The search criteria.
	 * @param type     The class type of the documents.
	 * @param <T>      The type of the documents.
	 * @return A list of matching documents.
	 */
	public <T> List<T> list(Criteria criteria, Class<T> type) {
		return list(Query.query(criteria), type);
	}

	/**
	 * Updates documents in the collection based on criteria.
	 *
	 * @param update     The update operation.
	 * @param criteria   The search criteria.
	 * @param type       The class type of the documents.
	 * @param collection The collection name where the documents are stored.
	 * @return The result of the update operation.
	 */
	public UpdateResult update(Criteria criteria, Update update, Class<?> type, String collection) {
		return mongoTemplate.updateMulti(Query.query(criteria), update, type, collection);
	}

	/**
	 * Updates documents in the default collection based on criteria.
	 *
	 * @param update   The update operation.
	 * @param criteria The search criteria.
	 * @param type     The class type of the documents.
	 * @return The result of the update operation.
	 */
	public UpdateResult update(Criteria criteria, Update update, Class<?> type) {
		return mongoTemplate.updateMulti(Query.query(criteria), update, type);
	}

	/**
	 * Updates documents in the default collection based on criteria.
	 *
	 * @param update   The update operation.
	 * @param criteria The search criteria.
	 * @param type     The class type of the documents.
	 * @return The result of the update operation.
	 */
	public UpdateResult upsert(Criteria criteria, Update update, Class<?> type) {
		return mongoTemplate.upsert(Query.query(criteria), update, type);
	}
	
	/**
	 * Updates documents in the default collection based on criteria.
	 *
	 * @param update   The update operation.
	 * @param criteria The search criteria.
	 * @param type     The class type of the documents.
	 * @return The result of the update operation.
	 */
	public UpdateResult upsert(Criteria criteria, Update update, Class<?> type, String collection) {
		return mongoTemplate.upsert(Query.query(criteria), update, type, collection);
	}
	
	/**
	 * Deletes documents in the collection based on criteria.
	 *
	 * @param criteria   The search criteria.
	 * @param type       The class type of the documents.
	 * @param collection The collection name where the documents are stored.
	 * @return The result of the delete operation.
	 */
	public DeleteResult delete(Criteria criteria, Class<?> type, String collection) {
		return mongoTemplate.remove(Query.query(criteria), type, collection);
	}

	/**
	 * Deletes documents in the default collection based on criteria.
	 *
	 * @param criteria The search criteria.
	 * @param type     The class type of the documents.
	 * @return The result of the delete operation.
	 */
	public DeleteResult delete(Criteria criteria, Class<?> type) {
		return mongoTemplate.remove(Query.query(criteria), type);
	}
}
