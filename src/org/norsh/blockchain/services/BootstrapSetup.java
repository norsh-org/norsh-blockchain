package org.norsh.blockchain.services;

import java.util.Map;

import org.norsh.blockchain.S;
import org.norsh.blockchain.components.NorshConstants;
import org.norsh.blockchain.config.BlockchainConfig;
import org.norsh.blockchain.model.elements.ElementDoc;
import org.norsh.blockchain.model.elements.ElementStatus;
import org.norsh.blockchain.model.utils.DynamicSequenceDoc;
import org.norsh.exceptions.OperationException;
import org.norsh.model.types.ElementType;
import org.norsh.model.types.Networks;
import org.norsh.security.Hasher;
import org.norsh.security.Signature;
import org.norsh.util.Converter;
import org.norsh.util.Strings;

/**
 * Bootstrap Service for Initial Blockchain Setup.
 * <p>
 * This service performs the initial bootstrap of the Norsh blockchain by creating the first Element,
 * its associated Transaction, Block, and initiating the mining process. It is designed to run only once
 * during the system's initial configuration.
 * </p>
 *
 * <h2>Bootstrap Process Overview:</h2>
 * <ul>
 *   <li>Checks if the blockchain has been initialized by verifying the existence of the dynamic sequence for elements.</li>
 *   <li>Creates the NSH Coin Element with fixed parameters (symbol, decimals, supply) and hardcoded secrets, hash, and signature.</li>
 *   <li>Generates an initial Transaction for the NSH Coin Element using hardcoded hash and signature values.</li>
 *   <li>Validates and creates the Element in the blockchain, overriding its timestamp and status for bootstrap purposes.</li>
 *   <li>Sets the metadata for the created Element.</li>
 *   <li>Retrieves a block associated with a specified Transaction ID, closes the block, and then mines it.</li>
 *   <li>Verifies the mined block and rewards the miner by invoking the mining service.</li>
 * </ul>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
public class BootstrapSetup {
	/**
	 * Starts the bootstrap process for initial blockchain configuration.
	 * <p>
	 * This method checks if the initial dynamic sequence for elements exists; if not, it creates the NSH Coin Element
	 * with its associated Transaction and metadata, then initializes the first Block and mining process.
	 * </p>
	 */
	public void start() {
//		BlockDoc block = minerService.mineBlock(mongoMain.id("c9f42f687165cb889640d0f9345085ddb1b44e31f7e26a1e5c986e7b391309ce", BlockDoc.class), 10, 10);
//		mongoMain.save(block);
		//minerService.verifyBlockAndRewardMiner(null, null, null, null)
		
		if (S.dynamicSequenceTemplate.id("elements") == null) {
			String owner = Hasher.sha3Hex(Converter.base64OrHexToBytes(BlockchainConfig.getInstance().get("publicKey")));
			
			{
				ElementDoc element = new ElementDoc();
				element.setType(ElementType.COIN);
				element.setOwner(owner);
				element.setSymbol(S.norshCoin.getSymbol());
				element.setDecimals(S.norshCoin.getDecimals());
				element.setInitialSupply(S.norshCoin.getInitialSupply());
				element.setTfo(BlockchainConfig.getInstance().get("nshTFO"));
				element.setPublicKey(BlockchainConfig.getInstance().get("publicKey"));
				element.setHash(Hasher.sha256Hex(Strings.concatenate(element.getSymbol(), element.getDecimals(), element.getInitialSupply(), element.getTfo(), element.getPublicKey())));
				element.setTimestamp(System.currentTimeMillis());
				element.setPrivacy(false);
				element.setVersion(1);
				element.setStatus(ElementStatus.ENABLED);
				
				element.setMetadata(Map.of(
						"name", "Norsh",
						"site", "https://norsh.org"
						));
				
				String signature = Signature.signHash(BlockchainConfig.getInstance().get("privateKey"), element.getHash());
				element.setSignature(signature);
				
				if (!Signature.verifyHash(element.getPublicKey(), element.getSignature(), element.getHash())) {
		            throw new OperationException("Invalid signature: The provided signature does not match the computed hash from the given public key.");
		        }
				
				DynamicSequenceDoc dynamicSequence = S.dynamicSequenceService.get(NorshConstants.getTagElements());
				element.setPreviousId(dynamicSequence.getData());
				element.setId(Hasher.sha3Hex(Strings.concatenate(element.getPreviousId(), element.getHash(), element.getTimestamp())));
	
				S.elementTemplate.save(element);
				S.dynamicSequenceService.set(NorshConstants.getTagElements(), element.getId());
				
				S.norshCoin.reload();
			}
			
			{
				ElementDoc element = new ElementDoc();
				element.setType(ElementType.PROXY);
				element.setOwner(owner);
				element.setSymbol("USDN-P");
				element.setDecimals(6);
				element.setPublicKey(BlockchainConfig.getInstance().get("publicKey"));
				element.setHash(Hasher.sha256Hex(Strings.concatenate(element.getSymbol(), element.getDecimals(), element.getInitialSupply(), element.getTfo(), element.getPublicKey())));
				element.setTimestamp(System.currentTimeMillis());
				element.setPrivacy(false);
				element.setVersion(1);
				element.setStatus(ElementStatus.ENABLED);
				
				element.setMetadata(Map.of(
						"name", "USD Norsh Proxy",
						"site", "https://norsh.org"
						));
				
				element.setMonitoredNetworks(Map.of("0x9E00eecbD1B387C01E7C8A449dF1FDbA0caa5B4e", Networks.ETHEREUM));
				
				String signature = Signature.signHash(BlockchainConfig.getInstance().get("privateKey"), element.getHash());
				element.setSignature(signature);
				
				if (!Signature.verifyHash(element.getPublicKey(), element.getSignature(), element.getHash())) {
		            throw new OperationException("Invalid signature: The provided signature does not match the computed hash from the given public key.");
		        }
				
				DynamicSequenceDoc dynamicSequence = S.dynamicSequenceService.get(NorshConstants.getTagElements());
				element.setPreviousId(dynamicSequence.getData());
				element.setId(Hasher.sha3Hex(Strings.concatenate(element.getPreviousId(), element.getHash(), element.getTimestamp())));
	
				S.elementTemplate.save(element);
				S.dynamicSequenceService.set(NorshConstants.getTagElements(), element.getId());
			}
		}
	}
}
