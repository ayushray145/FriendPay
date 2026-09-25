package com.splitledger.dashboard;

import com.splitledger.expense.ExpenseRepository;
import com.splitledger.expense.PersonExpenseBalanceProjection;
import com.splitledger.ledger.DebtDirection;
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

    public DashboardService(ExpenseRepository expenseRepository, SettlementRepository settlementRepository) {
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
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
