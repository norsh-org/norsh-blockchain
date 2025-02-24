package org.norsh.blockchain.docs.treasure;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Treasure Document.
 * <p>
 * This document records the "treasure" for an element, representing both the circulating supply
 * and the reserve of a pegged asset (if applicable).
 * </p>
 * <p>
 * <b>Volume Interpretation:</b>
 * <ul>
 *   <li>If {@code peg} is {@code null}, then {@code volume} corresponds to the total number of tokens 
 *       issued (i.e., mint minus burn, if any).</li>
 *   <li>If {@code peg} is a hexadecimal address, then {@code volume} represents the reserve amount 
 *       of the pegged asset held in the element's vault.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Note:</b> There will be two records for each element in this collection:
 * one with {@code peg} set to {@code null} (representing the circulating supply) and
 * another with a specific {@code peg} value (representing the reserve).
 * </p>
 *
 * @since 1.0.0
 */
@Getter
@Setter
@Document("treasure")
public class TreasureDoc {
    @Id
    private String id;

    /**
     * Identifier of the element for which the treasure is recorded.
     */
    private String element;

    /**
     * Peg reference used for reserve valuation.
     * <p>
     * If this field is {@code null}, the {@code volume} represents the circulating supply.
     * If it contains a hexadecimal address, the {@code volume} represents the reserve of the pegged asset.
     * </p>
     */
    private String peg;

    /**
     * The volume associated with the treasure.
     * <p>
     * Its interpretation depends on the {@code peg} field:
     * <ul>
     *   <li>If {@code peg} is {@code null}, it denotes the total tokens issued (mint minus burn).</li>
     *   <li>If {@code peg} is not {@code null}, it indicates the reserve amount of the pegged asset.</li>
     * </ul>
     * </p>
     */
    private BigDecimal volume;
    
    private BigDecimal circulation;
}
