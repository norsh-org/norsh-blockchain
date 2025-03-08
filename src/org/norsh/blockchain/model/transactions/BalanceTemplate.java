package org.norsh.blockchain.model.transactions;

import org.norsh.blockchain.S;
import org.norsh.blockchain.model.AbstractMongoTemplate;

public class BalanceTemplate extends AbstractMongoTemplate<BalanceDoc> {
	public BalanceTemplate() {
		super(S.mongoDatabase, BalanceDoc.class, "balances");
	}
}