package com.splitledger.settlement;

import com.splitledger.expense.ExpenseRepository;
import com.splitledger.ledger.DebtDirection;
import com.splitledger.person.Person;
import com.splitledger.person.PersonNotFoundException;
import com.splitledger.person.PersonRepository;
import com.splitledger.security.ApplicationUserNotFoundException;
import com.splitledger.user.AppUser;
import com.splitledger.user.AppUserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final SettlementRepository settlementRepository;
    private final ExpenseRepository expenseRepository;
    private final PersonRepository personRepository;
    private final AppUserRepository appUserRepository;

    public SettlementService(SettlementRepository settlementRepository, ExpenseRepository expenseRepository,
                             PersonRepository personRepository, AppUserRepository appUserRepository) {
        this.settlementRepository = settlementRepository;
        this.expenseRepository = expenseRepository;
        this.personRepository = personRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public SettlementResponse create(UUID ownerId, UUID personId, CreateSettlementRequest request) {
        Person person = personRepository.findOwnedPersonForUpdate(personId, ownerId)
                .orElseThrow(() -> new PersonNotFoundException(personId));
        BigDecimal outstanding = amountOrZero(expenseRepository.sumAmountByOwnerAndPersonAndDirection(
                ownerId, personId, request.paymentDirection()))
                .subtract(amountOrZero(settlementRepository.sumAmountByOwnerAndPersonAndDirection(
                        ownerId, personId, request.paymentDirection())));
        if (request.amount().compareTo(outstanding) > 0) {
            throw new SettlementExceedsOutstandingException(personId, outstanding);
        }

        AppUser owner = appUserRepository.findById(ownerId)
                .orElseThrow(() -> new ApplicationUserNotFoundException(ownerId));
        Instant settledAt = request.settledAt() == null ? Instant.now() : request.settledAt();
        Settlement settlement = new Settlement(owner, person, request.amount(), request.paymentDirection(), settledAt);
        return SettlementResponse.from(settlementRepository.save(settlement));
    }

    @Transactional(readOnly = true)
    public List<SettlementResponse> history(UUID ownerId, UUID personId) {
        verifyOwnership(ownerId, personId);
        return settlementRepository.findAllByOwnerIdAndPersonIdOrderBySettledAtDescIdDesc(ownerId, personId)
                .stream().map(SettlementResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PersonLedgerEntryResponse> ledger(UUID ownerId, UUID personId) {
        verifyOwnership(ownerId, personId);
        List<PersonLedgerEntryResponse> entries = new ArrayList<>();
        expenseRepository.findAllByOwnerIdAndPersonIdOrderByOccurredAtDescIdDesc(ownerId, personId).forEach(expense ->
                entries.add(new PersonLedgerEntryResponse(expense.getId(), "EXPENSE", expense.getAmount(),
                        expense.getDebtDirection(), expense.getDescription(), expense.getOccurredAt())));
        settlementRepository.findAllByOwnerIdAndPersonIdOrderBySettledAtDescIdDesc(ownerId, personId).forEach(settlement ->
                entries.add(new PersonLedgerEntryResponse(settlement.getId(), "SETTLEMENT", settlement.getAmount(),
                        settlement.getPaymentDirection(), "Settlement", settlement.getSettledAt())));
        entries.sort(Comparator.comparing(PersonLedgerEntryResponse::occurredAt).reversed()
                .thenComparing(PersonLedgerEntryResponse::id, Comparator.reverseOrder()));
        return List.copyOf(entries);
    }

    private void verifyOwnership(UUID ownerId, UUID personId) {
        if (personRepository.findByIdAndOwnerId(personId, ownerId).isEmpty()) {
            throw new PersonNotFoundException(personId);
        }
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount == null ? ZERO : amount;
    }
}
