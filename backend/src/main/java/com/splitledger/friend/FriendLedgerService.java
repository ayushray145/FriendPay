package com.splitledger.friend;

import com.splitledger.ledger.DebtDirection;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendLedgerService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final FriendExpenseProposalRepository proposalRepository;
    private final FriendSettlementRepository settlementRepository;

    public FriendLedgerService(FriendExpenseProposalRepository proposalRepository,
                               FriendSettlementRepository settlementRepository) {
        this.proposalRepository = proposalRepository;
        this.settlementRepository = settlementRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal sharedOutstanding(UUID ownerId, UUID friendId, DebtDirection direction) {
        BigDecimal approvedExpenses = proposalRepository.findAllByRequesterIdAndRecipientIdAndStatus(
                        ownerId, friendId, FriendExpenseProposalStatus.APPROVED).stream()
                .filter(proposal -> proposal.getDebtDirection() == direction)
                .map(FriendExpenseProposal::getAmount).reduce(ZERO, BigDecimal::add);
        approvedExpenses = approvedExpenses.add(proposalRepository.findAllByRequesterIdAndRecipientIdAndStatus(
                        friendId, ownerId, FriendExpenseProposalStatus.APPROVED).stream()
                .filter(proposal -> reverse(proposal.getDebtDirection()) == direction)
                .map(FriendExpenseProposal::getAmount).reduce(ZERO, BigDecimal::add));

        UUID payerId = direction == DebtDirection.PERSON_OWES_USER ? friendId : ownerId;
        UUID recipientId = direction == DebtDirection.PERSON_OWES_USER ? ownerId : friendId;
        BigDecimal sharedSettlements = amountOrZero(settlementRepository.sumByPayerAndRecipient(payerId, recipientId));
        return approvedExpenses.subtract(sharedSettlements).max(ZERO);
    }

    @Transactional(readOnly = true)
    public BigDecimal sharedSettlementAmount(UUID ownerId, UUID friendId, DebtDirection direction) {
        UUID payerId = direction == DebtDirection.PERSON_OWES_USER ? friendId : ownerId;
        UUID recipientId = direction == DebtDirection.PERSON_OWES_USER ? ownerId : friendId;
        return amountOrZero(settlementRepository.sumByPayerAndRecipient(payerId, recipientId));
    }

    public DebtDirection reverse(DebtDirection direction) {
        return direction == DebtDirection.PERSON_OWES_USER
                ? DebtDirection.USER_OWES_PERSON : DebtDirection.PERSON_OWES_USER;
    }

    private BigDecimal amountOrZero(BigDecimal amount) { return amount == null ? ZERO : amount; }
}
