package com.splitledger.dashboard;

import com.splitledger.expense.ExpenseRepository;
import com.splitledger.expense.PersonExpenseBalanceProjection;
import com.splitledger.friend.FriendSettlementBalanceProjection;
import com.splitledger.friend.FriendSettlementRepository;
import com.splitledger.ledger.DebtDirection;
import com.splitledger.person.Person;
import com.splitledger.person.PersonRepository;
import com.splitledger.settlement.PersonSettlementBalanceProjection;
import com.splitledger.settlement.SettlementRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final FriendSettlementRepository friendSettlementRepository;
    private final PersonRepository personRepository;

    public DashboardService(ExpenseRepository expenseRepository, SettlementRepository settlementRepository,
                            FriendSettlementRepository friendSettlementRepository,
                            PersonRepository personRepository) {
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
        this.friendSettlementRepository = friendSettlementRepository;
        this.personRepository = personRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID ownerId) {
        BigDecimal totalOwedToYou = ZERO;
        BigDecimal totalYouOwe = ZERO;
        List<DashboardPersonBalanceResponse> people = new java.util.ArrayList<>();
        java.util.Map<UUID, BigDecimal> settlementAdjustments = new java.util.HashMap<>();
        for (PersonSettlementBalanceProjection adjustment : settlementRepository.findBalanceAdjustmentsByOwnerId(
                ownerId, DebtDirection.PERSON_OWES_USER)) {
            settlementAdjustments.put(adjustment.getPersonId(), adjustment.getBalanceAdjustment());
        }
        java.util.Map<UUID, UUID> personIdsByLinkedUserId = new java.util.HashMap<>();
        for (Person person : personRepository.findAllByOwnerIdOrderByDisplayNameAsc(ownerId)) {
            if (person.getLinkedUser() != null) {
                personIdsByLinkedUserId.put(person.getLinkedUser().getId(), person.getId());
            }
        }
        for (FriendSettlementBalanceProjection adjustment : friendSettlementRepository.findBalanceAdjustmentsByOwnerId(ownerId)) {
            boolean ownerPaid = adjustment.getPayerUserId().equals(ownerId);
            UUID friendId = ownerPaid ? adjustment.getRecipientUserId() : adjustment.getPayerUserId();
            UUID personId = personIdsByLinkedUserId.get(friendId);
            BigDecimal balanceAdjustment = ownerPaid ? adjustment.getAmount() : adjustment.getAmount().negate();
            if (personId != null) settlementAdjustments.merge(personId, balanceAdjustment, BigDecimal::add);
        }

        for (PersonExpenseBalanceProjection balance : expenseRepository.findBalancesByOwnerId(
                ownerId, DebtDirection.PERSON_OWES_USER)) {
            BigDecimal net = balance.getNetBalance().add(
                    settlementAdjustments.getOrDefault(balance.getPersonId(), ZERO));
            if (net.signum() == 0) {
                continue;
            }
            if (net.signum() > 0) {
                totalOwedToYou = totalOwedToYou.add(net);
            } else {
                totalYouOwe = totalYouOwe.add(net.abs());
            }
            people.add(new DashboardPersonBalanceResponse(
                    balance.getPersonId(), balance.getDisplayName(), net));
        }

        return new DashboardResponse(
                totalOwedToYou, totalYouOwe, totalOwedToYou.subtract(totalYouOwe), List.copyOf(people));
    }
}
