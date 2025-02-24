package org.norsh.blockchain.components;

import org.norsh.blockchain.docs.elements.ElementDoc;
import org.norsh.blockchain.services.database.MongoMain;
import org.norsh.model.types.ElementType;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

/**
 * Service for managing balances associated with owners and tokens.
 * <p>
 * Provides functionality to retrieve and initialize balances while maintaining consistency and supporting the creation
 * of default balance records.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 * <li>Efficient retrieval and initialization of balances.</li>
 * <li>Support for default balance creation with zero amount.</li>
 * <li>Integration with logging for better traceability.</li>
 * </ul>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
@Component
public final class NorshCoin {
	private MongoMain mongoMain;
	private ElementDoc elementDoc;

	public String getSymbol() {
		return "NSH";
	}

	public Integer getDecimals() {
		return 18;
	}

	public Long getInitialSupply() {
		return 45_000_000l;
	}

	public NorshCoin(MongoMain mongoMain) {
		this.mongoMain = mongoMain;
		reload();
	}

	public String getId() {
		return elementDoc.getId();
	}

	public String getOwner() {
		return elementDoc.getOwner();
	}

	public String getPublicKey() {
		return elementDoc.getPublicKey();
	}

	public Boolean reload() {
		synchronized (mongoMain) {
			if (elementDoc == null)
				elementDoc = mongoMain.get(Criteria.where("symbol").is(getSymbol()).and("type").is(ElementType.COIN), ElementDoc.class);
			
			return elementDoc != null;
		}
	}
}
