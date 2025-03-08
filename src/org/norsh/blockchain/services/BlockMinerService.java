package org.norsh.blockchain.services;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.bson.conversions.Bson;
import org.norsh.blockchain.S;
import org.norsh.blockchain.components.NorshConstants;
import org.norsh.blockchain.model.blockchain.BlockDoc;
import org.norsh.security.Hasher;
import org.norsh.util.Strings;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

/**
 * Service for mining blocks in the Norsh blockchain.
 * <p>
 * This service utilizes multi-threading to perform Proof-of-Work (PoW) mining by generating 
 * hashes and comparing them against a difficulty criterion. The mining process attempts to 
 * find a hash that starts with a specific number of leading zeroes, defined by the block's 
 * difficulty.
 * </p>
 * 
 * <h2>Mining Logic:</h2>
 * <ul>
 *   <li>Generates hashes by combining the block data with a list of nonces.</li>
 *   <li>Distributes mining tasks across multiple threads for parallel processing.</li>
 *   <li>Stops when a valid hash is found or the specified depth is reached.</li>
 * </ul>
 * 
 * <h2>Thread Management:</h2>
 * <ul>
 *   <li>Uses a fixed thread pool to optimize resource utilization.</li>
 *   <li>Supports batch processing of nonces for efficient computation.</li>
 *   <li>Ensures thread-safe updates to the block state when a valid hash is found.</li>
 * </ul>
 * 
 * @license NCL-R
 * @author Danthur Lice
 * @since 01/2025
 * @version 1.0
 * @see BlockDoc
 */
public class BlockMinerService {
	/**
	 * Mines a block using multi-threaded Proof-of-Work (PoW) methodology.
	 * 
	 * @param block the block to be mined.
	 * @param threads the number of threads to use for mining.
	 * @param maxNonceDepth the maximum depth of nonce combinations to explore.
	 * @return the mined block, or {@code null} if no valid hash is found within the depth limit.
	 */
	public BlockDoc mineBlock(BlockDoc block, Integer threads, Integer maxNonceDepth) {
		int NONCE_BATCH_SIZE = 10_000;

		// Prefix defining the required leading zeroes for a valid hash
		String difficultyPrefix = "0".repeat(block.getDifficulty());

		List<Long> nonces = new ArrayList<>();
		nonces.add(0L); // Initialize the first nonce

		String blockHashBase = Strings.concatenate(
				block.getId(), 
				block.getTimestamp(), 
				block.getMerkleRoot(), 
				block.getPreviousBlockHash(), 
				block.getMiningReleaseTimestamp()
				);

		ExecutorService executor = Executors.newFixedThreadPool(threads);

		if (block.getMined() == null)
			block.setMined(false);

		while (!block.getMined()) {
			if (nonces.size() > maxNonceDepth) {
				break;
			}

			final LinkedList<List<Long>> nonceBatches = new LinkedList<>();
			// Generate nonce batches up to the depth limit
			for (int i = 0; i < NONCE_BATCH_SIZE; i++) {
				List<Long> currentNonce = new ArrayList<>(nonces);
				nonceBatches.add(currentNonce);
				incrementNonces(nonces);
			}

			executor.submit(new Thread() {
				@Override
				public void run() {
					try {
						for (List<Long> batchNonces : nonceBatches) {
							if (block.getMined())
								return;

							String hashInput = Hasher.sha256Hex(Strings.concatenate(blockHashBase, batchNonces));

							// Check if the hash satisfies the difficulty
							if (hashInput.startsWith(difficultyPrefix)) {
								synchronized (block) {
									System.out.println(Strings.concatenate(blockHashBase, batchNonces));
									block.setMined(true);
									block.setNonces(batchNonces); // Save the nonces used
									block.setBlockHash(hashInput);
									return;
								}
							}
						}
					} finally {
						// Cleanup logic can be added here
					}
				}
			});
		}

		executor.shutdown();

		try {
			if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Executor threads did not finish in a reasonable time");
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException("Thread interrupted while waiting for tasks to complete", e);
		}

		return block;
	}

	/**
	 * Increments the nonce values for multi-dimensional nonce handling.
	 * 
	 * @param nonces the list of nonce dimensions to increment.
	 */
	private static void incrementNonces(List<Long> nonces) {
		for (int i = nonces.size() - 1; i >= 0; i--) {
			long currentValue = nonces.get(i);

			if (currentValue + 1 < Long.MAX_VALUE) {
				nonces.set(i, currentValue + 1); // Increment current dimension
				return; // Increment completed
			} else {
				nonces.set(i, 0L); // Reset current dimension
			}
		}

		// If all dimensions were reset, add a new dimension
		nonces.add(0, 0L);
	}

	/**
	 * Validates a mined block's hash and rewards the miner if valid.
	 * 
	 * @param blockId the ID of the block to validate.
	 * @param nonces the list of nonces used in mining.
	 * @param providedMinedHash the hash obtained from the mining process.
	 * @param miner the address of the miner to be recorded and rewarded.
	 * @return {@code true} if the provided mined hash is valid and the miner is rewarded; {@code false} otherwise.
	 */
	public Boolean verifyBlockAndRewardMiner(String blockId, List<Long> nonces, String providedMinedHash, String miner) {
		return S.semaphoreService.execute(NorshConstants.getSemaphoreBlockchain(), _ -> {
			BlockDoc block = S.blockTemplate.id(blockId);
			if (block.getMined()) {
				return false;
			}

			String blockHashBase = Strings.concatenate(
					block.getId(), 
					block.getTimestamp(), 
					block.getMerkleRoot(), 
					block.getPreviousBlockHash(), 
					block.getMiningReleaseTimestamp(), 
					nonces
					);
			String computedHash = Hasher.sha256Hex(blockHashBase);
			String difficultyPrefix = "0".repeat(block.getDifficulty());

			if (computedHash.equals(providedMinedHash) && computedHash.startsWith(difficultyPrefix)) {
				Bson update = Updates.combine(
						Updates.set("miner", miner),
						Updates.set("mined", true),
						Updates.set("miningEndTimestamp", System.currentTimeMillis()),
						Updates.set("nonces", nonces),
						Updates.set("blockHash", providedMinedHash));

				S.blockTemplate.update(Filters.eq("_id", blockId), update);

				distributeMiningReward(miner, block);
				return true;
			} 
			return false;
		});
	}

	private void distributeMiningReward(String minerName, BlockDoc block) {
		System.out.println("Miner " + minerName + " rewarded for block " + block.getId());
	}
}
