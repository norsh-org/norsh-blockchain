package org.norsh.blockchain.model;

import java.util.ArrayList;
import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
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
 *   <li>Supports listing, updating, and deleting documents using native MongoDB filters.</li>
 *   <li>Direct interaction with MongoDB without Spring Data.</li>
 * </ul>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 */
public abstract class AbstractMongoTemplate<T extends ADoc> {
    private final MongoDatabase database;
    private final Class<T> type;
    private final String collectionName;
    
    public AbstractMongoTemplate(MongoDatabase database, Class<T> type, String collectionName) {
    	this.collectionName = collectionName;
        this.database = database;
        this.type = type;
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public Class<T> getType() {
        return type;
    }

    public String getCollectionName() {
		return collectionName;
	}
    
    /**
     * Saves a document in MongoDB.
     *
     * @param doc        The document to be saved.
     * @param collection The collection name where the document will be stored.
     * @return The saved document.
     */
    public T save(T doc, String collection) {
        MongoCollection<T> mongoCollection = database.getCollection(collection, getType());

        if (doc.getId() != null) {
            mongoCollection.replaceOne(Filters.eq("_id", doc.getId()), doc, new ReplaceOptions().upsert(true));
        } else {
            InsertOneResult result = mongoCollection.insertOne(doc);
            doc.setId(result.getInsertedId().asString().getValue());
        }

        return doc;
    }

    public T save(T doc) {
        return save(doc, getCollectionName());
    }

    /**
     * Retrieves a document by its ID.
     */
    public T id(Object id, String collection) {
        return get(Filters.eq("_id", id), collection);
    }

    public T id(Object id) {
        return id(id, getCollectionName());
    }

    /**
     * Retrieves a document based on a query filter.
     */
    public T get(Bson filter, String collection) {
        MongoCollection<T> mongoCollection = database.getCollection(collection, getType());
        return mongoCollection.find(filter).first();
    }

    public T get(Bson filter) {
        return get(filter, getCollectionName());
    }

    /**
     * Checks if a document exists in the collection.
     */
    public boolean exists(Bson filter, String collection) {
        MongoCollection<T> mongoCollection = database.getCollection(collection, getType());
        return mongoCollection.countDocuments(filter) > 0;
    }

    public boolean exists(Bson filter) {
        return exists(filter, getCollectionName());
    }

    /**
     * Retrieves a list of documents based on a filter.
     */
    public List<T> list(Bson filter, String collection) {
        MongoCollection<T> mongoCollection = database.getCollection(collection, getType());
        FindIterable<T> results = mongoCollection.find(filter);
        
        List<T> list = new ArrayList<>();
        for (T doc : results) {
            list.add(doc);
        }
        return list;
    }
    
    public List<T> list() {
        MongoCollection<T> mongoCollection = database.getCollection(getCollectionName(), getType());
        FindIterable<T> results = mongoCollection.find();
        
        List<T> list = new ArrayList<>();
        for (T doc : results) {
            list.add(doc);
        }
        
        return list;
    }


    public List<T> list(Bson filter) {
        return list(filter, getCollectionName());
    }

    /**
     * Updates a document based on a filter.
     */
    public UpdateResult update(Bson filter, Bson update, String collection) {
        MongoCollection<T> mongoCollection = database.getCollection(collection, getType());
        return mongoCollection.updateOne(filter, update);
    }

    public UpdateResult update(Bson filter, Bson update) {
        return update(filter, update, getCollectionName());
    }

    /**
     * Updates multiple documents based on a filter.
     */
    public UpdateResult updateMany(Bson filter, Bson update, String collection) {
        MongoCollection<T> mongoCollection = database.getCollection(collection, getType());
        return mongoCollection.updateMany(filter, update);
    }

    public UpdateResult updateMany(Bson filter, Bson update) {
        return updateMany(filter, update, getCollectionName());
    }

    /**
     * Performs an upsert (update if exists, insert if not).
     */
    public UpdateResult upsert(Bson filter, Bson update, String collection) {
        MongoCollection<T> mongoCollection = database.getCollection(collection, getType());
        return mongoCollection.updateOne(filter, update, new UpdateOptions().upsert(false));
    }

    public UpdateResult upsert(Bson filter, Bson update) {
        return upsert(filter, update, getCollectionName());
    }

    /**
     * Deletes a single document based on a filter.
     */
    public DeleteResult deleteOne(Bson filter, String collection) {
        MongoCollection<T> mongoCollection = database.getCollection(collection, getType());
        return mongoCollection.deleteOne(filter);
    }

    public DeleteResult deleteOne(Bson filter) {
        return deleteOne(filter, getCollectionName());
    }

    /**
     * Deletes multiple documents based on a filter.
     */
    public DeleteResult deleteMany(Bson filter, String collection) {
        MongoCollection<T> mongoCollection = database.getCollection(collection, getType());
        return mongoCollection.deleteMany(filter);
    }

    public DeleteResult deleteMany(Bson filter) {
        return deleteMany(filter, getCollectionName());
    }
}
