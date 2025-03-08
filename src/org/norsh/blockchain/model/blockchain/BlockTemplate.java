package org.norsh.blockchain.model.blockchain;

import org.norsh.blockchain.S;
import org.norsh.blockchain.model.AbstractMongoTemplate;

public class BlockTemplate extends AbstractMongoTemplate<BlockDoc> {
	public BlockTemplate() {
		super(S.mongoDatabase, BlockDoc.class, "blocks");
	}
}