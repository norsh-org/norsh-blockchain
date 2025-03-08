package org.norsh.blockchain.model.transactions;

import org.norsh.blockchain.S;
import org.norsh.blockchain.model.AbstractMongoTemplate;

public class TransactionTemplate extends AbstractMongoTemplate<TransactionDoc> {
	public TransactionTemplate() {
		super(S.mongoDatabase, TransactionDoc.class, "ledger");
	}
}