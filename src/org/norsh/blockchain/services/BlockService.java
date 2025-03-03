package org.norsh.blockchain.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.norsh.blockchain.components.NorshConstants;
import org.norsh.blockchain.config.BlockchainConfig;
import org.norsh.blockchain.docs.blockchain.BlockDoc;
import org.norsh.blockchain.docs.blockchain.BlockTransactionDoc;
import org.norsh.blockchain.docs.transactions.TransactionDoc;
import org.norsh.blockchain.docs.utils.DynamicSequenceDoc;
import org.norsh.blockchain.services.database.MongoMain;
import org.norsh.blockchain.services.database.MongoRead;
import org.norsh.blockchain.services.utils.DynamicSequenceService;
import org.norsh.blockchain.services.utils.SemaphoreService;
import org.norsh.config.DefaultsConfig;
import org.norsh.security.Hasher;
import org.norsh.util.Log;
import org.norsh.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.mongodb.client.result.UpdateResult;

/**
 * Block Management Service.
 * <p>
 * This service manages the blocks within the Norsh blockchain. It handles block creation, adding transactions to blocks,
 * and finalizing (closing) blocks. It ensures proper computation of the Merkle root, determines block difficulty,
 * and maintains sequential consistency of the blockchain.
 * </p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
@Service
public class BlockService {
    private DefaultsConfig defaultsConfig = BlockchainConfig.getInstance().getDefaultsConfig();
    
    @Autowired
    private Log log;

    @Autowired
    private MongoMain mongoMain;

    @Autowired
    private MongoRead mongoRead;

    @Autowired
    private SemaphoreService semaphoreService;

    @Autowired
    private DynamicSequenceService dynamicSequenceService;

    /**
     * Adds a transaction to the current block.
     * <p>
     * This method calculates the current block number based on a fixed time interval and attempts to add the given
     * transaction to an open block. If an open block exists, the transaction is appended; if not, a new block is created.
     * In case of temporary failures (e.g., no open block available), the method will retry with an exponential backoff.
     * </p>
     *
     * @param transaction the {@link TransactionDoc} to add to the block.
     * @return the block number (Long) where the transaction was successfully added.
     */
    public synchronized Long addTransactionToBlock(TransactionDoc transaction) {
        Long resultBlockNumber = null;
        AtomicInteger retries = new AtomicInteger(0);

        do {
            resultBlockNumber = semaphoreService.execute(NorshConstants.getSemaphoreBlockchain(), _ -> {
                Long blockNumber = calculateBlockNumber();

                BlockTransactionDoc blockTransaction = new BlockTransactionDoc();
                blockTransaction.setId(transaction.getId());
                blockTransaction.setElement(transaction.getElement());
                blockTransaction.setPrivacy(transaction.getPrivacy() == null ? false : transaction.getPrivacy());
                blockTransaction.setLedger(transaction.getLedger());
                blockTransaction.setTax(transaction.getTotalTax());

                if (blockTransaction.getPrivacy())
                    blockTransaction.setVolume(transaction.getTotal());

                Update update = new Update();
                update.push("transactions", blockTransaction);
                Criteria criteria = Criteria.where("number").is(blockNumber).and("closed").is(false);
                
                UpdateResult updateResult = mongoMain.update(criteria, update, BlockDoc.class);

                // First, try to insert the transaction into an existing open block.
                // If successful, return the block number.
                if (updateResult.getModifiedCount() > 0) {
                    return blockNumber;
                }

                // If no open block is found, attempt to create a new block.
                // If a block already exists, continue looping until the transaction is successfully inserted.
                if (!mongoMain.exists(Criteria.where("number").is(blockNumber), BlockDoc.class)) {
                    DynamicSequenceDoc dynamicSequence = dynamicSequenceService.get(NorshConstants.getSequenceBlockId());

                    String previousId = dynamicSequence.getData();
                    String id = Hasher.sha3Hex(Strings.concatenate(previousId, blockNumber));

                    dynamicSequenceService.inc(NorshConstants.getSequenceBlockId(), id);

                    BlockDoc block = new BlockDoc();
                    block.setId(id);
                    block.setPreviousId(previousId);
                    block.setNumber(blockNumber);
                    block.setHeight(dynamicSequence.getSequence());
                    block.setClosed(false);
                    block.setMined(false);
                    block.setTimestamp(System.currentTimeMillis());
                    block.setTransactions(new LinkedList<>());

                    // Close the previous block if applicable.
                    if (block.getPreviousId() != null) {
                        closeBlock(block.getPreviousId());
                    }

                    mongoMain.save(block);
                }
                
                return null;
            });

            int r = retries.getAndIncrement();
            if (r > 0) {
                try {
                    Thread.sleep(Math.min(defaultsConfig.getThreadInitialBackoffMs() * r, defaultsConfig.getThreadMaxBackoffMs()));
                } catch (Exception ignored) {
                }
            }
            
            
        } while (resultBlockNumber == null);

        return resultBlockNumber;
    }

    /**
     * Finds a block that contains a specific transaction.
     *
     * @param transactionId the ID of the transaction.
     * @return the {@link BlockDoc} that includes the transaction, or {@code null} if no such block exists.
     */
    public BlockDoc findBlockByTransactionId(String transactionId) {
        return mongoRead.get(Criteria.where("transactions._id").is(transactionId), BlockDoc.class);
    }

    /**
     * Closes a block identified by its ID.
     * <p>
     * This method finalizes the block by:
     * <ul>
     *   <li>Calculating its Merkle root from the included transactions.</li>
     *   <li>Setting the close timestamp and block difficulty.</li>
     *   <li>Marking the block as closed and computing the total transaction fees.</li>
     * </ul>
     * The updated block is then persisted in the database.
     * </p>
     *
     * @param id the ID of the block to close.
     */
    private void closeBlock(String id) {
        if (id == null)
            return;

        BlockDoc block = mongoMain.id(id, BlockDoc.class);
        if (block == null) {
            log.warning("Block not found for ID", id);
            return;
        }

        if (block.getClosed()) {
            return;
        }

        // Set the mining release timestamp for the first block; for subsequent blocks, link with the last mined block.
        if (block.getHeight() == 0) {
            block.setMiningReleaseTimestamp(System.currentTimeMillis());
        } else {
            BlockDoc lastBlock = getLastMinedBlock(block.getHeight());
            if (lastBlock != null && lastBlock.getBlockHash() != null) {
                block.setPreviousBlockHash(lastBlock.getBlockHash());
                block.setMiningReleaseTimestamp(System.currentTimeMillis());
            }
        }

        block.setMerkleRoot(calculateMerkleRoot(block.getTransactions()));
        block.setCloseTimestamp(System.currentTimeMillis());
        block.setDifficulty(getDifficulty(block));
        block.setClosed(true);

        block.setTotalFee(
            block.getTransactions() == null || block.getTransactions().isEmpty() 
                ? BigDecimal.ZERO 
                : block.getTransactions().stream()
                        .map(BlockTransactionDoc::getTax)
                        .filter(fee -> fee != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        mongoMain.save(block);

        log.debug("Block closed successfully", block.getId());
    }

    /**
     * Calculates the Merkle root from a list of block transactions.
     * <p>
     * The Merkle root is computed by iteratively hashing pairs of transaction IDs until a single hash remains.
     * If there are no transactions, {@code null} is returned.
     * </p>
     *
     * @param transactions the list of transactions in the block.
     * @return the Merkle root hash, or {@code null} if the transactions list is empty.
     */
    private String calculateMerkleRoot(List<BlockTransactionDoc> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            log.debug("No transactions found for Merkle root calculation", null);
            return null;
        }

        List<String> transactionIds = transactions.stream().map(BlockTransactionDoc::getId).toList();

        while (transactionIds.size() > 1) {
            List<String> nextLevel = new ArrayList<>();

            for (int i = 0; i < transactionIds.size(); i += 2) {
                String left = transactionIds.get(i);
                String right = (i + 1 < transactionIds.size()) ? transactionIds.get(i + 1) : left;
                nextLevel.add(Hasher.sha3Hex(left + right));
            }

            transactionIds = nextLevel;
        }

        String merkleRoot = transactionIds.get(0);
        log.debug("Merkle root calculated", merkleRoot);
        return merkleRoot;
    }

    /**
     * Calculates the current block number based on a fixed time interval.
     * <p>
     * The current timestamp is divided by the block window interval (in milliseconds) to determine the block number.
     * This ensures that blocks are generated at regular, fixed intervals.
     * </p>
     *
     * @return the current block number.
     */
    public Long calculateBlockNumber() {
        Long timeInterval = Duration.ofMinutes(NorshConstants.getBlockWindowIntervalMin()).toMillis();
        return System.currentTimeMillis() / timeInterval;
    }

    /**
     * Calculates the difficulty level for a block.
     * <p>
     * The difficulty is defined as twice the number of digits in the integer part of the sum of all transaction taxes
     * within the block. For example, if the total tax is 12345.172, the integer part is 12345, which has 5 digits,
     * so the difficulty is 5 * 2 = 10.
     * This implementation uses a logarithmic approach (log10) to determine the number of digits.
     * </p>
     *
     * @param block the block whose transactions are used for difficulty calculation.
     * @return the calculated difficulty level.
     */
    private int getDifficulty(BlockDoc block) {
        BigDecimal totalTax = BigDecimal.ZERO;
        if (block.getTransactions() != null) {
            for (BlockTransactionDoc t : block.getTransactions()) {
                if (t.getTax() != null) {
                    totalTax = totalTax.add(t.getTax());
                }
            }
        }
        // Extract the integer part of totalTax.
        long integerPart = totalTax.setScale(0, RoundingMode.DOWN).longValue();
        // Determine the number of digits using log10. For integerPart = 0, set digits to 1.
        int numDigits = (integerPart == 0) ? 1 : (int) (Math.floor(Math.log10(integerPart)) + 1);
        return numDigits * 2;
    }

    /**
     * Retrieves the last mined block preceding a given block height.
     * <p>
     * This method decrements the provided height by one and searches for a block at that height that has been mined.
     * If no such block exists (or if the resulting height is less than zero), {@code null} is returned.
     * </p>
     *
     * @param height the current block height.
     * @return the last mined {@link BlockDoc} or {@code null} if none is found.
     */
    public BlockDoc getLastMinedBlock(Long height) {
        height = height - 1;
        if (height < 0)
            return null;

        return mongoMain.get(Criteria.where("height").is(height).and("mined").is(true), BlockDoc.class);
    }

    /**
     * Releases the next block for mining.
     * <p>
     * This method searches for a block at the next height that is closed, not yet mined, and does not have a previous block hash assigned.
     * If such a block is found, its mining release timestamp is updated and its previous block hash is set to the provided value.
     * </p>
     *
     * @param height the height of the last mined block.
     * @param previousBlockHash the hash of the last mined block.
     */
    public void releaseNextBlockForMining(Long height, String previousBlockHash) {
        height = height + 1;
        BlockDoc blockDoc = mongoMain.get(
            Criteria.where("height").is(height)
                    .and("mined").is(false)
                    .and("closed").is(true)
                    .and("previousBlockHash").exists(false),
            BlockDoc.class
        );

        if (blockDoc != null) {
            Update update = new Update();
            update.set("miningReleaseTimestamp", System.currentTimeMillis());
            update.set("previousBlockHash", previousBlockHash);

            mongoMain.update(Criteria.where("_id").is(blockDoc.getId()), update, BlockDoc.class);
        }
    }
}
