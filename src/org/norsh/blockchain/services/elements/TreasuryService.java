//package org.norsh.blockchain.services.elements;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//
//import org.norsh.blockchain.components.NorshCoin;
//import org.norsh.blockchain.model.elements.ElementDoc;
//import org.norsh.blockchain.model.templates.MongoMain;
//import org.norsh.blockchain.model.templates.MongoRead;
//import org.norsh.blockchain.model.transactions.BalanceDoc;
//import org.norsh.blockchain.services.BalanceService;
//import org.norsh.blockchain.services.queue.MessagingResponseService;
//import org.norsh.blockchain.services.transactions.TransactionService;
//import org.norsh.blockchain.services.utils.DynamicSequenceService;
//import org.norsh.blockchain.services.utils.SemaphoreService;
//import org.norsh.exceptions.OperationStatus;
//import org.norsh.model.dtos.elements.TreasuryInfoDto;
//import org.norsh.model.transport.DataTransfer;
//import org.norsh.model.transport.Processable;
//import org.norsh.rest.RestMethod;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
///**
// * Smart Element Management Service.
// * <p>
// * This service handles the creation, retrieval, update, and metadata management of Smart Elements. It enforces business
// * rules, ensures transactional integrity via distributed semaphores, and securely stores elements in MongoDB. It also
// * communicates with external services via message queues.
// * </p>
// *
// * <h2>Main Responsibilities:</h2>
// * <ul>
// * <li>Create Smart Elements with transactional integrity.</li>
// * <li>Retrieve Smart Elements by ID.</li>
// * <li>Update metadata and policies according to business rules.</li>
// * <li>Manage network associations for elements, specifically for PROXY type elements.</li>
// * </ul>
// *
// * @since 1.0.0
// * @version 1.0.0
// * @author Danthur Lice
// * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
// */
//@Service
////@Processable({ElementCreateDto.class, ElementGetDto.class, ElementMetadataDto.class, ElementNetworkDto.class})
//public class TreasuryService {
//	@Autowired
//	private NorshCoin norshCoin;
//	
//	@Autowired
//	private BalanceService balanceService;
//
//	@Autowired
//	private DynamicSequenceService dynamicSequenceService;
//
//	@Autowired
//	private SemaphoreService semaphoreService;
//
//	@Autowired
//	private MongoMain mongoMain;
//
//	@Autowired
//	private MongoRead mongoRead;
//
//	@Autowired
//	private MessagingResponseService messagingService;
//
//	@Autowired
//	private TransactionService transactionService;
//
//
////	private void createTreasure(ElementDoc element) {
////		semaphoreService.execute(element.getId(), _ -> {
////			BalanceDoc elementBalanceNorsh = balanceService.get(element.getId(), norshCoin.getId());
////			balanceService.set(elementBalanceNorsh, BigDecimal.ZERO);
////
////			BalanceDoc elementBalanceSupply = balanceService.get(element.getId(), element.getId());
////			balanceService.set(elementBalanceSupply, BigDecimal.valueOf(element.getSupply()));
////		});
////	}
//
//	@Processable(method = RestMethod.POST)
//	public DataTransfer getTreasury(TreasuryInfoDto dto) {
//		ElementDoc element = mongoMain.id(dto.getId(), ElementDoc.class);
//		if (element == null) {
//			return messagingService.response(dto.getId(), OperationStatus.NOT_FOUND);
//		}
//
//		TreasuryInfoDto treasuryInfo = new TreasuryInfoDto();
//		treasuryInfo.setId(element.getId());
//
//		// Retrieve balances of the element and NSH
//		BalanceDoc elementBalance = balanceService.get(element.getId(), element.getId());
//		BalanceDoc nshBalance = balanceService.get(element.getId(), norshCoin.getId());
//
//		// Get total supply and circulating supply
//		treasuryInfo.setSupply(BigDecimal.valueOf(element.getInitialSupply()));
//		treasuryInfo.setNsh(nshBalance.getAmount());
//		treasuryInfo.setCirculating(treasuryInfo.getSupply().subtract(elementBalance.getAmount())); // Tokens actually in circulation
//
//		// Validations
//		if (treasuryInfo.getCirculating().compareTo(BigDecimal.ZERO) < 0) {
//			return messagingService.response(dto.getId(), OperationStatus.ERROR);
//		}
//
//		if (treasuryInfo.getCirculating().compareTo(BigDecimal.ZERO) == 0 || treasuryInfo.getSupply().compareTo(BigDecimal.ZERO) <= 0) {
//			treasuryInfo.setValue(BigDecimal.ZERO);
//			return messagingService.response(dto.getId(), treasuryInfo);
//		}
//
//		// Calculate circulation price
//
//		BigDecimal circulationPrice = treasuryInfo.getNsh().divide(treasuryInfo.getCirculating(), norshCoin.getDecimals(), RoundingMode.HALF_UP); // Adjusted precision
//
//		// Calculate Treasury price
//		treasuryInfo.setValue(circulationPrice.add(treasuryInfo.getNsh().divide(treasuryInfo.getSupply(), norshCoin.getDecimals(), RoundingMode.HALF_UP)));
//
//		return messagingService.response(dto.getId(), treasuryInfo);
//	}
//}
