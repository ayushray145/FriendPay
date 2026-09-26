package com.splitledger.settlement;

import com.splitledger.expense.ExpenseRepository;
import com.splitledger.friend.FriendLedgerService;
import com.splitledger.friend.FriendRequestRepository;
import com.splitledger.friend.FriendRequestStatus;
import com.splitledger.friend.FriendSettlement;
import com.splitledger.friend.FriendSettlementRepository;
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
    private final FriendLedgerService friendLedgerService;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendSettlementRepository friendSettlementRepository;

    public SettlementService(SettlementRepository settlementRepository, ExpenseRepository expenseRepository,
                             PersonRepository personRepository, AppUserRepository appUserRepository,
                             FriendLedgerService friendLedgerService,
                             FriendRequestRepository friendRequestRepository,
                             FriendSettlementRepository friendSettlementRepository) {
        this.settlementRepository = settlementRepository;
        this.expenseRepository = expenseRepository;
        this.personRepository = personRepository;
        this.appUserRepository = appUserRepository;
        this.friendLedgerService = friendLedgerService;
        this.friendRequestRepository = friendRequestRepository;
        this.friendSettlementRepository = friendSettlementRepository;
    }

    @Transactional
    public SettlementResponse create(UUID ownerId, UUID personId, CreateSettlementRequest request) {
        Person person = personRepository.findOwnedPersonForUpdate(personId, ownerId)
                .orElseThrow(() -> new PersonNotFoundException(personId));
        BigDecimal expenses = amountOrZero(expenseRepository.sumAmountByOwnerAndPersonAndDirection(
                ownerId, personId, request.paymentDirection()));
        BigDecimal localSettlements = amountOrZero(settlementRepository.sumAmountByOwnerAndPersonAndDirection(
                ownerId, personId, request.paymentDirection()));
        AppUser friend = person.getLinkedUser();
        if (friend != null) {
            var friendship = friendRequestRepository.findBetweenUsersWithStatus(ownerId, friend.getId(),
                    FriendRequestStatus.ACCEPTED).orElse(null);
            if (friendship != null) {
                throw new com.splitledger.friend.FriendConflictException(
                        "Payments between accepted friends must be reported and approved by the recipient");
            }
        }
        BigDecimal sharedSettlements = friend == null ? ZERO
                : friendLedgerService.sharedSettlementAmount(ownerId, friend.getId(), request.paymentDirection());
        BigDecimal outstanding = expenses.subtract(localSettlements).subtract(sharedSettlements);
        if (request.amount().compareTo(outstanding) > 0) {
            throw new SettlementExceedsOutstandingException(personId, outstanding);
        }

        AppUser owner = appUserRepository.findById(ownerId)
                .orElseThrow(() -> new ApplicationUserNotFoundException(ownerId));
        Instant settledAt = request.settledAt() == null ? Instant.now() : request.settledAt();
        Settlement privateSettlement = settlementRepository.save(new Settlement(
                owner, person, request.amount(), request.paymentDirection(), settledAt));
        return new SettlementResponse(privateSettlement.getId(), personId, request.amount(),
                request.paymentDirection(), settledAt);
    }

    @Transactional(readOnly = true)
    public List<SettlementResponse> history(UUID ownerId, UUID personId) {
        verifyOwnership(ownerId, personId);
        List<SettlementResponse> history = new ArrayList<>(settlementRepository
                .findAllByOwnerIdAndPersonIdOrderBySettledAtDescIdDesc(ownerId, personId)
                .stream().map(SettlementResponse::from).toList());
        Person person = personRepository.findByIdAndOwnerId(personId, ownerId).orElseThrow();
        if (person.getLinkedUser() != null) {
            for (FriendSettlement settlement : friendSettlementRepository.findAllBetweenUsers(
                    ownerId, person.getLinkedUser().getId())) {
                history.add(sharedSettlementResponse(settlement, ownerId, personId));
            }
        }
        history.sort(Comparator.comparing(SettlementResponse::settledAt).reversed()
                .thenComparing(SettlementResponse::id, Comparator.reverseOrder()));
        return List.copyOf(history);
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
        Person person = personRepository.findByIdAndOwnerId(personId, ownerId).orElseThrow();
        if (person.getLinkedUser() != null) {
            friendSettlementRepository.findAllBetweenUsers(ownerId, person.getLinkedUser().getId()).forEach(settlement ->
                    entries.add(new PersonLedgerEntryResponse(settlement.getId(), "SETTLEMENT", settlement.getAmount(),
                            settlement.getPayer().getId().equals(ownerId)
                                    ? DebtDirection.USER_OWES_PERSON : DebtDirection.PERSON_OWES_USER,
                            "Confirmed friend payment", settlement.getSettledAt())));
        }
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

    private SettlementResponse sharedSettlementResponse(FriendSettlement settlement, UUID ownerId, UUID personId) {
        DebtDirection direction = settlement.getPayer().getId().equals(ownerId)
                ? DebtDirection.USER_OWES_PERSON : DebtDirection.PERSON_OWES_USER;
        return new SettlementResponse(settlement.getId(), personId, settlement.getAmount(), direction,
                settlement.getSettledAt());
    }
}
