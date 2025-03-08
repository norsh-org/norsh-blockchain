package org.norsh.blockchain;

import org.norsh.blockchain.components.NorshCoin;
import org.norsh.blockchain.config.BlockchainConfig;
import org.norsh.blockchain.config.MongoConnectionConfig;
import org.norsh.blockchain.model.blockchain.BlockTemplate;
import org.norsh.blockchain.model.elements.ElementTemplate;
import org.norsh.blockchain.model.transactions.BalanceTemplate;
import org.norsh.blockchain.model.transactions.TransactionTemplate;
import org.norsh.blockchain.model.utils.DynamicSequenceTemplate;
import org.norsh.blockchain.services.BalanceService;
import org.norsh.blockchain.services.BlockService;
import org.norsh.blockchain.services.elements.ElementService;
import org.norsh.blockchain.services.queue.CacheService;
import org.norsh.blockchain.services.queue.Dispatcher;
import org.norsh.blockchain.services.transactions.TransactionService;
import org.norsh.blockchain.services.utils.DynamicSequenceService;
import org.norsh.blockchain.services.utils.SemaphoreService;
import org.norsh.config.Config;
import org.norsh.util.Log;

import com.mongodb.client.MongoDatabase;

public class S {
	public static final Config config;
	public static final Log log;
	public static final MongoConnectionConfig mongoConnnectionConfig;
	public static final MongoDatabase mongoDatabase;
	public static final CacheService cacheService;
	
	public static final DynamicSequenceTemplate dynamicSequenceTemplate;
	public static final ElementTemplate elementTemplate;
	public static final BalanceTemplate balanceTemplate;
	public static final TransactionTemplate transactionTemplate;
	public static final BlockTemplate blockTemplate;
	
	
	
	public static final SemaphoreService semaphoreService;
	public static final DynamicSequenceService dynamicSequenceService;
	public static final ElementService elementService;
	public static final BalanceService balanceService;
	public static final TransactionService transactionService;
	public static final BlockService blockService;
	
	
	public static final Dispatcher dispatcher;
	public static final NorshCoin norshCoin;
	
	static {
		config = BlockchainConfig.getInstance();
		log = Log.getInstance(config);
		
		mongoConnnectionConfig = new MongoConnectionConfig();
		mongoDatabase = mongoConnnectionConfig.getDatabase();
		cacheService = new CacheService();
		
		dynamicSequenceTemplate = new DynamicSequenceTemplate();
		elementTemplate = new ElementTemplate();
		balanceTemplate = new BalanceTemplate();
		transactionTemplate = new TransactionTemplate();
		blockTemplate = new BlockTemplate();
		
		semaphoreService = new SemaphoreService();
		dynamicSequenceService = new DynamicSequenceService();
		elementService = new ElementService();
		balanceService = new BalanceService();
		transactionService = new TransactionService();
		blockService = new BlockService();
		
		dispatcher = new Dispatcher();
		norshCoin = new NorshCoin();

		dispatcher.loadDispatcherService();
		norshCoin.reload();
	}
}
