package org.norsh.blockchain.components;

import org.norsh.blockchain.S;
import org.norsh.blockchain.model.elements.ElementDoc;
import org.norsh.model.types.ElementType;

import com.mongodb.client.model.Filters;

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
public final class NorshCoin {
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
		synchronized (NorshCoin.class) {
			if (elementDoc == null)
				elementDoc = S.elementTemplate.get(Filters.and(Filters.eq("symbol", getSymbol()), Filters.eq("type", ElementType.COIN)));
			
			return elementDoc != null;
		}
	}
}
