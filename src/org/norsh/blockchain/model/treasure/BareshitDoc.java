//package org.norsh.blockchain.model.docs.treasure;
//
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.mapping.Document;
//
//import lombok.Getter;
//import lombok.Setter;
//
///**
// * Represents the genesis minting event for Elements in the Norsh blockchain.
// * <p>
// * This document, named "bareshit", records all initial mint events of elements during the creation phase of the blockchain.
// * Each mint event is linked to its predecessor, forming a chain that represents the genesis lineage of the system.
// * </p>
// *
// *
// * @since 1.0.0
// * @version 1.0.0
// * @author Danthur Lice
// * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
// */
//@Getter
//@Setter
//@Document("bareshit")
//public class BareshitDoc {
//    /**
//     * Unique identifier for the mint event.
//     */
//    @Id
//    private String id;
//
//    /**
//     * Identifier of the previous mint event.
//     */
//    private String previousId;
//
//    /**
//     * Reference or identifier of the element being minted.
//     */
//    private String element;
//
//    /**
//     * The volume (amount) of tokens minted in this event.
//     */
//    private Long volume;
//
//    /**
//     * Timestamp when the mint event occurred.
//     */
//    private Long timestamp;
//
//    /**
//     * Public key of the creator (minter) of the element.
//     */
//    private String publicKey;
//
//    /**
//     * Cryptographic signature that validates the mint event.
//     */
//    private String signature;
//
//    /**
//     * Version of the document structure.
//     */
//    private Integer version;
//}


