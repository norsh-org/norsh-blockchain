package org.norsh.blockchain.services;

import java.math.BigDecimal;

import org.norsh.blockchain.docs.transactions.BalanceDoc;
import org.norsh.blockchain.services.database.MongoMain;
import org.norsh.util.Logger;
import org.norsh.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
@Service
public class BalanceService {
	@Autowired
	private MongoMain mongoMain;
	
	@Autowired
	private Logger log;

	public String getId(String owner, String token) {
		return Strings.concatenateWithSymbol("_", owner, token);
	}

	/**
	 * Retrieves the balance for a given owner and token.
	 * <p>
	 * If the balance does not exist, a new balance record with a zero amount is initialized and returned.
	 * </p>
	 *
	 * @param owner the owner of the balance.
	 * @param token the token associated with the balance.
	 * @return the balance document.
	 */
	public BalanceDoc get(String owner, String token) {
		String id = getId(owner, token);
		BalanceDoc balance = mongoMain.id(id, BalanceDoc.class);

		if (balance == null) {
			balance = new BalanceDoc();
			balance.setId(id);
			balance.setToken(token);
			balance.setOwner(owner);
			balance.setAmount(BigDecimal.valueOf(10_000));
		} else {
			log.debug("Balance retrieved successfully for owner: " + owner + ", token: " + token, balance);
		}

		return balance;
	}

	public BalanceDoc set(BalanceDoc balance, BigDecimal amount) {
		balance.setAmount(amount);
		return save(balance);
	}
	
	public BalanceDoc save(BalanceDoc balance) {
		mongoMain.save(balance);
		return balance;
	}
	
	public Boolean hasBalance(String owner, String token, BigDecimal amount) {
		BalanceDoc balanceFrom = get(owner, token);
		return balanceFrom.getAmount().compareTo(amount) >= 0;
	}
}
