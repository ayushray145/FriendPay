package com.splitledger.settlement;

import com.splitledger.expense.ExpenseRepository;
import com.splitledger.friend.FriendLedgerService;
import com.splitledger.friend.FriendRequest;
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
        FriendRequest friendship = null;
        AppUser friend = person.getLinkedUser();
        BigDecimal sharedOutstanding = ZERO;
        if (friend != null) {
            friendship = friendRequestRepository.findBetweenUsersWithStatus(ownerId, friend.getId(),
                    FriendRequestStatus.ACCEPTED).orElse(null);
            if (friendship != null) {
                sharedOutstanding = friendLedgerService.sharedOutstanding(
                        ownerId, friend.getId(), request.paymentDirection());
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
        BigDecimal sharedAmount = friendship == null ? ZERO : request.amount().min(sharedOutstanding);
        FriendSettlement sharedSettlement = null;
        if (sharedAmount.signum() > 0) {
            AppUser payer = request.paymentDirection() == DebtDirection.PERSON_OWES_USER ? friend : owner;
            AppUser recipient = request.paymentDirection() == DebtDirection.PERSON_OWES_USER ? owner : friend;
            sharedSettlement = friendSettlementRepository.save(new FriendSettlement(
                    friendship, payer, recipient, sharedAmount, settledAt, owner));
        }
        BigDecimal privateAmount = request.amount().subtract(sharedAmount);
        Settlement privateSettlement = null;
        if (privateAmount.signum() > 0) {
            privateSettlement = settlementRepository.save(new Settlement(
                    owner, person, privateAmount, request.paymentDirection(), settledAt));
        }
        UUID settlementId = sharedSettlement != null ? sharedSettlement.getId() : privateSettlement.getId();
        return new SettlementResponse(settlementId, personId, request.amount(), request.paymentDirection(), settledAt);
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
