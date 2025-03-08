package org.norsh.blockchain.services.utils;

import java.util.ArrayList;
import java.util.List;

import org.bson.conversions.Bson;
import org.norsh.blockchain.S;
import org.norsh.blockchain.model.utils.DynamicSequenceDoc;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

/**
 * Service for managing dynamic sequences with support for both incremental and manual updates.
 * <p>
 * This service provides mechanisms to retrieve, update, and increment sequence values while ensuring data consistency
 * and flexibility.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 * <li>Efficient management of sequential and non-sequential data.</li>
 * <li>Flexible updates for sequence and associated data fields.</li>
 * <li>Centralized logic for updates to reduce redundancy and improve maintainability.</li>
 * <li>Atomic operations to ensure consistency in concurrent environments.</li>
 * </ul>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 */
public class DynamicSequenceService {
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
		DynamicSequenceDoc dynamicSequence = S.dynamicSequenceTemplate.id(id);
		if (dynamicSequence == null) {
			dynamicSequence = new DynamicSequenceDoc();
			dynamicSequence.setId(id);
			dynamicSequence.setSequence(0L);
			S.dynamicSequenceTemplate.save(dynamicSequence);

			S.log.debug("Dynamic sequence not found, creating a new instance: " + id);
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
	private void update(String id, Long sequence, String data, boolean increment, boolean updateData) {
		//DynamicSequenceDoc dynamicSequence = get(id);

		List<Bson> updates = new ArrayList<Bson>();

		if (increment) {
			updates.add(Updates.inc("sequence", 1));
		} else if (sequence != null) {
			updates.add(Updates.set("sequence", sequence));
		}

		if (updateData) {
			if (data != null) {
				updates.add(Updates.set("data", data));
			} else {
				updates.add(Updates.unset("data"));
			}
		}

		if (updates.size() > 0) {
			UpdateResult result = S.dynamicSequenceTemplate.update(Filters.eq("_id", id), Updates.combine(updates.toArray(new Bson[0])));

			if (result.getMatchedCount() <= 0) {
				S.log.warning("Failed to update dynamic sequence: " + id);
			}
		}

		// Return the updated document
		//return dynamicSequence;
	}

	/**
	 * Sets a new sequence value and/or data for a specific ID.
	 *
	 * @param id       the ID of the sequence to update.
	 * @param sequence the new sequence value.
	 * @param data     the new data value.
	 * @return The updated {@link DynamicSequenceDoc}.
	 */
	public void set(String id, Long sequence, String data) {
		update(id, sequence, data, false, true);
	}

	/**
	 * Sets a new sequence value for a specific ID.
	 *
	 * @param id       the ID of the sequence to update.
	 * @param sequence the new sequence value.
	 * @return The updated {@link DynamicSequenceDoc}.
	 */
	public void set(String id, Long sequence) {
		update(id, sequence, null, false, false);
	}

	/**
	 * Sets new data for a specific ID.
	 *
	 * @param id   the ID of the sequence to update.
	 * @param data the new data value.
	 * @return The updated {@link DynamicSequenceDoc}.
	 */
	public void set(String id, String data) {
		update(id, null, data, false, true);
	}

	/**
	 * Increments the sequence value and optionally updates the data for a specific ID.
	 *
	 * @param id   the ID of the sequence to update.
	 * @param data the new data value.
	 * @return The updated {@link DynamicSequenceDoc}.
	 */
	public void inc(String id, String data) {
		update(id, null, data, true, true);
	}

	/**
	 * Increments the sequence value for a specific ID.
	 *
	 * @param id the ID of the sequence to update.
	 * @return The updated {@link DynamicSequenceDoc}.
	 */
	public void inc(String id) {
		update(id, null, null, true, false);
	}
}
