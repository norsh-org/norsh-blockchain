package org.norsh.blockchain.model.utils;

import org.norsh.blockchain.model.ADoc;

import lombok.Getter;
import lombok.Setter;

/**
 * Document representing a mechanism for managing sequential and contextual data.
 * <p>
 * This class supports two primary use cases:
 * <ul>
 *   <li>Managing an auto-incrementing sequence for tracking operations or events.</li>
 *   <li>Storing custom contextual data for manual assignment and retrieval in concurrent operations.</li>
 * </ul>
 * <p>
 * The {@code id} field uniquely identifies the record, and the {@code sequence} field provides an incremental counter
 * for sequential operations. The {@code data} field allows manual storage and retrieval of additional contextual information.
 * </p>
 *
 * @license NCL-R
 * @since 01/2025
 * @version 1.0
 * @author Danthur Lice
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
@Getter
@Setter
public class DynamicSequenceDoc extends ADoc {

    /**
     * Unique identifier for the record.
     * <p>
     * This ID corresponds to the specific sequence or context being tracked.
     * </p>
     */
    private String id;

    /**
     * Incremental sequence value.
     * <p>
     * This field automatically increments with each operation, enabling consistent ordering
     * for operations or events linked to this record.
     * </p>
     */
    private Long sequence;

    /**
     * Contextual data associated with the sequence.
     * <p>
     * This field stores manually assigned information, which can be updated or retrieved
     * across multiple operations. It complements the sequence for use cases where non-incremental
     * data needs to be stored.
     * </p>
     */
    private String data;
}
