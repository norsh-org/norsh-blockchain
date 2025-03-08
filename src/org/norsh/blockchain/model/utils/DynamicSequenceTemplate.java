package org.norsh.blockchain.model.utils;

import org.norsh.blockchain.S;
import org.norsh.blockchain.model.AbstractMongoTemplate;

public class DynamicSequenceTemplate extends AbstractMongoTemplate<DynamicSequenceDoc> {
	public DynamicSequenceTemplate() {
		super(S.mongoDatabase, DynamicSequenceDoc.class, "sequences");
	}
}