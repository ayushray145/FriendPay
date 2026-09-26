package com.splitledger.expense;

import com.splitledger.ledger.DebtDirection;
import com.splitledger.friend.FriendLedgerService;
import com.splitledger.person.Person;
import com.splitledger.person.PersonNotFoundException;
import com.splitledger.person.PersonRepository;
import com.splitledger.security.ApplicationUserNotFoundException;
import com.splitledger.settlement.SettlementRepository;
import com.splitledger.user.AppUser;
import com.splitledger.user.AppUserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    private static final BigDecimal ZERO_AMOUNT = new BigDecimal("0.00");

    private final ExpenseRepository expenseRepository;
    private final PersonRepository personRepository;
    private final AppUserRepository appUserRepository;
    private final SettlementRepository settlementRepository;
    private final FriendLedgerService friendLedgerService;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            PersonRepository personRepository,
            AppUserRepository appUserRepository,
            SettlementRepository settlementRepository,
            FriendLedgerService friendLedgerService) {
        this.expenseRepository = expenseRepository;
        this.personRepository = personRepository;
        this.appUserRepository = appUserRepository;
        this.settlementRepository = settlementRepository;
        this.friendLedgerService = friendLedgerService;
    }

    @Transactional
    public ExpenseResponse create(UUID ownerUserId, UUID personId, CreateExpenseRequest request) {
        AppUser owner = appUserRepository.findById(ownerUserId)
                .orElseThrow(() -> new ApplicationUserNotFoundException(ownerUserId));
        Person person = findOwnedPerson(ownerUserId, personId);
        DebtDirection direction = request.debtDirection() == null
                ? DebtDirection.PERSON_OWES_USER
                : request.debtDirection();
        Instant occurredAt = request.occurredAt() == null ? Instant.now() : request.occurredAt();

        Expense expense = new Expense(
                owner, person, request.amount(), direction, request.description().trim(), occurredAt);
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> history(UUID ownerUserId, UUID personId) {
        findOwnedPerson(ownerUserId, personId);
        return expenseRepository.findAllByOwnerIdAndPersonIdOrderByOccurredAtDescIdDesc(ownerUserId, personId)
                .stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(UUID ownerUserId, UUID personId, UUID expenseId) {
        findOwnedPerson(ownerUserId, personId);
        Expense expense = expenseRepository.findByIdAndOwnerIdAndPersonId(expenseId, ownerUserId, personId)
                .orElseThrow(() -> new ExpenseNotFoundException(expenseId));
        return ExpenseResponse.from(expense);
    }

    @Transactional(readOnly = true)
    public PersonBalanceResponse balance(UUID ownerUserId, UUID personId) {
        findOwnedPerson(ownerUserId, personId);
        BigDecimal personOwes = sumFor(ownerUserId, personId, DebtDirection.PERSON_OWES_USER)
                .subtract(settledFor(ownerUserId, personId, DebtDirection.PERSON_OWES_USER));
        BigDecimal userOwes = sumFor(ownerUserId, personId, DebtDirection.USER_OWES_PERSON)
                .subtract(settledFor(ownerUserId, personId, DebtDirection.USER_OWES_PERSON));
        return new PersonBalanceResponse(personId, personOwes.subtract(userOwes));
    }

    private Person findOwnedPerson(UUID ownerUserId, UUID personId) {
        return personRepository.findByIdAndOwnerId(personId, ownerUserId)
                .orElseThrow(() -> new PersonNotFoundException(personId));
    }

    private BigDecimal sumFor(UUID ownerUserId, UUID personId, DebtDirection direction) {
        BigDecimal sum = expenseRepository.sumAmountByOwnerAndPersonAndDirection(ownerUserId, personId, direction);
        return sum == null ? ZERO_AMOUNT : sum;
    }

    private BigDecimal settledFor(UUID ownerUserId, UUID personId, DebtDirection direction) {
        BigDecimal sum = settlementRepository.sumAmountByOwnerAndPersonAndDirection(ownerUserId, personId, direction);
        BigDecimal localSettlements = sum == null ? ZERO_AMOUNT : sum;
        Person person = personRepository.findByIdAndOwnerId(personId, ownerUserId).orElseThrow();
        if (person.getLinkedUser() == null) return localSettlements;
        return localSettlements.add(friendLedgerService.sharedSettlementAmount(
                ownerUserId, person.getLinkedUser().getId(), direction));
    }
}
