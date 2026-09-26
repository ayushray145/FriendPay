package com.splitledger.friend;

import com.splitledger.expense.Expense;
import com.splitledger.expense.ExpenseRepository;
import com.splitledger.ledger.DebtDirection;
import com.splitledger.person.Person;
import com.splitledger.person.PersonRepository;
import com.splitledger.security.ApplicationUserNotFoundException;
import com.splitledger.user.AppUser;
import com.splitledger.user.AppUserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendExpenseService {

    private final FriendExpenseProposalRepository proposalRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final AppUserRepository userRepository;
    private final PersonRepository personRepository;
    private final ExpenseRepository expenseRepository;

    public FriendExpenseService(FriendExpenseProposalRepository proposalRepository,
                                FriendRequestRepository friendRequestRepository,
                                AppUserRepository userRepository,
                                PersonRepository personRepository,
                                ExpenseRepository expenseRepository) {
        this.proposalRepository = proposalRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.expenseRepository = expenseRepository;
    }

    @Transactional
    public FriendExpenseProposalResponse create(UUID requesterId, CreateFriendExpenseRequest request) {
        UUID recipientId = request.friendUserId();
        FriendRequest friendship = friendRequestRepository
                .findBetweenUsersWithStatus(requesterId, recipientId, FriendRequestStatus.ACCEPTED)
                .orElseThrow(() -> new FriendExpenseConflictException("Friend expenses require an accepted friendship"));
        AppUser requester = findUser(requesterId);
        AppUser recipient = findUser(recipientId);
        requireLinkedContact(requesterId, recipientId);
        FriendExpenseProposal proposal = new FriendExpenseProposal(friendship, requester, recipient,
                request.amount(), request.description().trim(), request.debtDirection());
        return FriendExpenseProposalResponse.from(proposalRepository.save(proposal), requesterId);
    }

    @Transactional(readOnly = true)
    public List<FriendExpenseProposalResponse> list(UUID viewerId) {
        return proposalRepository.findAllByRequesterIdOrRecipientIdOrderByCreatedAtDesc(viewerId, viewerId)
                .stream().filter(proposal -> proposal.getStatus() != FriendExpenseProposalStatus.APPROVED)
                .map(proposal -> FriendExpenseProposalResponse.from(proposal, viewerId)).toList();
    }

    @Transactional
    public FriendExpenseProposalResponse approve(UUID recipientId, UUID proposalId) {
        FriendExpenseProposal proposal = findForUpdate(proposalId);
        if (!proposal.getRecipient().getId().equals(recipientId)) {
            throw new FriendExpenseNotFoundException(proposalId);
        }
        if (proposal.getStatus() != FriendExpenseProposalStatus.PENDING) {
            throw new FriendExpenseConflictException("Only pending friend expenses can be approved");
        }
        UUID requesterId = proposal.getRequester().getId();
        UUID acceptedRecipientId = proposal.getRecipient().getId();
        if (friendRequestRepository.findBetweenUsersWithStatus(requesterId, acceptedRecipientId,
                FriendRequestStatus.ACCEPTED).isEmpty()) {
            throw new FriendExpenseConflictException("This friend connection is no longer active");
        }
        Person requesterContact = requireLinkedContact(requesterId, acceptedRecipientId);
        Person recipientContact = requireLinkedContact(acceptedRecipientId, requesterId);
        DebtDirection reverseDirection = reverse(proposal.getDebtDirection());
        expenseRepository.saveAll(List.of(
                new Expense(proposal.getRequester(), requesterContact, proposal.getAmount(),
                        proposal.getDebtDirection(), proposal.getDescription(), proposal.getOccurredAt()),
                new Expense(proposal.getRecipient(), recipientContact, proposal.getAmount(),
                        reverseDirection, proposal.getDescription(), proposal.getOccurredAt())));
        proposal.approve();
        return FriendExpenseProposalResponse.from(proposalRepository.save(proposal), recipientId);
    }

    @Transactional
    public FriendExpenseProposalResponse dispute(UUID recipientId, UUID proposalId, DisputeFriendExpenseRequest request) {
        FriendExpenseProposal proposal = findForUpdate(proposalId);
        if (!proposal.getRecipient().getId().equals(recipientId)) {
            throw new FriendExpenseNotFoundException(proposalId);
        }
        if (proposal.getStatus() != FriendExpenseProposalStatus.PENDING) {
            throw new FriendExpenseConflictException("Only pending friend expenses can be disputed");
        }
        proposal.dispute(request.reason().trim());
        return FriendExpenseProposalResponse.from(proposalRepository.save(proposal), recipientId);
    }

    @Transactional
    public FriendExpenseProposalResponse reviseAndResubmit(
            UUID requesterId, UUID proposalId, ReviseFriendExpenseRequest request) {
        FriendExpenseProposal proposal = findForUpdate(proposalId);
        if (!proposal.getRequester().getId().equals(requesterId)) {
            throw new FriendExpenseNotFoundException(proposalId);
        }
        if (proposal.getStatus() != FriendExpenseProposalStatus.DISPUTED) {
            throw new FriendExpenseConflictException("Only disputed friend expenses can be revised");
        }
        proposal.reviseAndResubmit(request.amount(), request.description().trim(), request.debtDirection());
        return FriendExpenseProposalResponse.from(proposalRepository.save(proposal), requesterId);
    }

    private FriendExpenseProposal findForUpdate(UUID proposalId) {
        return proposalRepository.findByIdForUpdate(proposalId)
                .orElseThrow(() -> new FriendExpenseNotFoundException(proposalId));
    }

    private AppUser findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new ApplicationUserNotFoundException(id));
    }

    private Person requireLinkedContact(UUID ownerId, UUID friendId) {
        return personRepository.findByOwnerIdAndLinkedUserId(ownerId, friendId)
                .orElseThrow(() -> new FriendExpenseConflictException("Accepted friend ledger contact not found"));
    }

    private DebtDirection reverse(DebtDirection direction) {
        return direction == DebtDirection.PERSON_OWES_USER
                ? DebtDirection.USER_OWES_PERSON : DebtDirection.PERSON_OWES_USER;
    }
}
