package org.norsh.blockchain.services.utils;

import org.norsh.blockchain.docs.utils.DynamicSequenceDoc;
import org.norsh.blockchain.services.database.MongoMain;
import org.norsh.util.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.mongodb.client.result.UpdateResult;

/**
 * Service for managing dynamic sequences with support for both incremental and manual updates.
 * <p>
 * This service provides mechanisms to retrieve, update, and increment sequence values while ensuring data
 * consistency and flexibility.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Efficient management of sequential and non-sequential data.</li>
 *   <li>Flexible updates for sequence and associated data fields.</li>
 *   <li>Centralized logic for updates to reduce redundancy and improve maintainability.</li>
 *   <li>Atomic operations to ensure consistency in concurrent environments.</li>
 * </ul>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 */
@Service
public class DynamicSequenceService {
    
    @Autowired
    private MongoMain mongoMain;

    @Autowired
    private Logger log;

    /**
     * Retrieves the {@link DynamicSequenceDoc} for a given ID.
     * <p>
     * If the sequence does not exist, a new one is created with an initial sequence value of 0.
     * </p>
     *
     * @param id the ID of the sequence to retrieve.
     * @return the {@link DynamicSequenceDoc} or a new instance if not found.
     */
    public DynamicSequenceDoc get(String id) {
        DynamicSequenceDoc dynamicSequence = mongoMain.id(id, DynamicSequenceDoc.class);
        if (dynamicSequence == null) {
            dynamicSequence = new DynamicSequenceDoc();
            dynamicSequence.setId(id);
            dynamicSequence.setSequence(0L);
            mongoMain.save(dynamicSequence);

            log.debug("Dynamic sequence not found, creating a new instance: " + id);
        }
        return dynamicSequence;
    }

    /**
     * Updates a sequence document with the provided parameters.
     * <p>
     * This method centralizes the logic for updating sequence values and associated data fields.
     * </p>
     *
     * @param id         the ID of the sequence to update.
     * @param sequence   the new sequence value, or {@code null} to leave unchanged.
     * @param data       the new data value, or {@code null} to unset.
     * @param increment  whether to increment the sequence value atomically.
     * @param updateData whether to update the data field.
     * @return The updated {@link DynamicSequenceDoc}.
     */
    private DynamicSequenceDoc update(String id, Long sequence, String data, boolean increment, boolean updateData) {
    	DynamicSequenceDoc dynamicSequence = get(id);
    	
        Update update = new Update();

        if (increment) {
            update.inc("sequence", 1);
        } else if (sequence != null) {
            update.set("sequence", sequence);
        }

        if (updateData) {
            if (data != null) {
                update.set("data", data);
            } else {
                update.unset("data");
            }
        }

        UpdateResult result = mongoMain.update(Criteria.where("_id").is(id), update, DynamicSequenceDoc.class);
        
        if (result.getMatchedCount() <= 0) {
            log.warning("Failed to update dynamic sequence: " + id);
        }

        // Return the updated document
        return dynamicSequence;
    }

    /**
     * Sets a new sequence value and/or data for a specific ID.
     *
     * @param id       the ID of the sequence to update.
     * @param sequence the new sequence value.
     * @param data     the new data value.
     * @return The updated {@link DynamicSequenceDoc}.
     */
    public DynamicSequenceDoc set(String id, Long sequence, String data) {
        return update(id, sequence, data, false, true);
    }

    /**
     * Sets a new sequence value for a specific ID.
     *
     * @param id       the ID of the sequence to update.
     * @param sequence the new sequence value.
     * @return The updated {@link DynamicSequenceDoc}.
     */
    public DynamicSequenceDoc set(String id, Long sequence) {
        return update(id, sequence, null, false, false);
    }

    /**
     * Sets new data for a specific ID.
     *
     * @param id   the ID of the sequence to update.
     * @param data the new data value.
     * @return The updated {@link DynamicSequenceDoc}.
     */
    public DynamicSequenceDoc set(String id, String data) {
        return update(id, null, data, false, true);
    }

    /**
     * Increments the sequence value and optionally updates the data for a specific ID.
     *
     * @param id   the ID of the sequence to update.
     * @param data the new data value.
     * @return The updated {@link DynamicSequenceDoc}.
     */
    public DynamicSequenceDoc inc(String id, String data) {
        return update(id, null, data, true, true);
    }

    /**
     * Increments the sequence value for a specific ID.
     *
     * @param id the ID of the sequence to update.
     * @return The updated {@link DynamicSequenceDoc}.
     */
    public DynamicSequenceDoc inc(String id) {
        return update(id, null, null, true, false);
    }
}
