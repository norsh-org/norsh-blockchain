package org.norsh.blockchain.model.elements;

import org.norsh.blockchain.S;
import org.norsh.blockchain.model.AbstractMongoTemplate;

public class ElementTemplate extends AbstractMongoTemplate<ElementDoc> {
	public ElementTemplate() {
		super(S.mongoDatabase, ElementDoc.class, "elements");
	}
}