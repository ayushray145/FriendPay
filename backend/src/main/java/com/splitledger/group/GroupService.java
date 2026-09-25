package com.splitledger.group;

import com.splitledger.security.ApplicationUserNotFoundException;
import com.splitledger.user.AppUser;
import com.splitledger.user.AppUserRepository;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupService {

    private final LedgerGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final GroupExpenseRepository expenseRepository;
    private final GroupExpenseSplitRepository splitRepository;
    private final GroupDisputeRepository disputeRepository;
    private final AppUserRepository userRepository;

    public GroupService(LedgerGroupRepository groupRepository, GroupMemberRepository memberRepository,
                        GroupExpenseRepository expenseRepository, GroupExpenseSplitRepository splitRepository,
                        GroupDisputeRepository disputeRepository,
                        AppUserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.expenseRepository = expenseRepository;
        this.splitRepository = splitRepository;
        this.disputeRepository = disputeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GroupResponse create(UUID ownerId, CreateGroupRequest request) {
        AppUser owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ApplicationUserNotFoundException(ownerId));
        LedgerGroup group = groupRepository.saveAndFlush(new LedgerGroup(owner, request.name().trim()));
        memberRepository.saveAndFlush(new GroupMember(
                group, owner, owner, GroupMemberRole.OWNER, GroupMemberStatus.ACTIVE));
        return response(group);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> list(UUID userId) {
        return groupRepository.findVisibleToUser(userId, GroupMemberStatus.ACTIVE).stream()
                .map(this::response)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupResponse get(UUID userId, UUID groupId) {
        return response(requireActiveGroup(userId, groupId));
    }

    @Transactional
    public GroupMemberResponse addMember(UUID ownerId, UUID groupId, AddGroupMemberRequest request) {
        LedgerGroup group = requireOwnerGroup(ownerId, groupId);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        AppUser addedUser = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new GroupAccountNotFoundException(email));
        if (addedUser.getId().equals(ownerId)) {
            throw new GroupConflictException("You are already a member of this group");
        }

        var existing = memberRepository.findByLedgerGroupIdAndUserId(groupId, addedUser.getId());
        if (existing.isPresent()) {
            GroupMember member = existing.get();
            if (member.getStatus() == GroupMemberStatus.REMOVED) {
                member.addAgain(group.getOwner());
                return GroupMemberResponse.from(memberRepository.save(member));
            }
            throw new GroupConflictException("The user is already a member of this group");
        }
        return GroupMemberResponse.from(memberRepository.saveAndFlush(new GroupMember(
                group, addedUser, group.getOwner(), GroupMemberRole.MEMBER, GroupMemberStatus.ACTIVE)));
    }

    @Transactional
    public void removeMember(UUID ownerId, UUID groupId, UUID memberId) {
        requireOwnerGroup(ownerId, groupId);
        GroupMember member = memberRepository.findByLedgerGroupIdAndUserId(groupId, memberId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));
        if (member.getRole() == GroupMemberRole.OWNER) {
            throw new GroupConflictException("The group owner cannot be removed");
        }
        member.remove();
    }

    @Transactional
    public GroupExpenseResponse createExpense(UUID userId, UUID groupId, CreateGroupExpenseRequest request) {
        LedgerGroup group = requireActiveGroup(userId, groupId);
        UUID payerId = request.paidByUserId() == null ? userId : request.paidByUserId();
        AppUser payer = userRepository.findById(payerId)
                .filter(candidate -> memberRepository.existsByLedgerGroupIdAndUserIdAndStatus(
                        groupId, candidate.getId(), GroupMemberStatus.ACTIVE))
                .orElseThrow(() -> new GroupNotFoundException(groupId));
        AppUser recordedBy = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationUserNotFoundException(userId));
        GroupExpense expense = new GroupExpense(group, payer, recordedBy, request.amount(),
                request.description().trim(), request.occurredAt());
        expense = expenseRepository.saveAndFlush(expense);

        List<AppUser> participants = resolveParticipants(groupId, request.participantUserIds());
        BigInteger[] centsAndRemainder = request.amount().movePointRight(2).toBigIntegerExact()
                .divideAndRemainder(BigInteger.valueOf(participants.size()));
        BigDecimal baseShare = new BigDecimal(centsAndRemainder[0], 2);
        List<GroupExpenseSplit> splits = new java.util.ArrayList<>();
        for (int index = 0; index < participants.size(); index++) {
            BigDecimal share = index < centsAndRemainder[1].intValueExact()
                    ? baseShare.add(new BigDecimal("0.01")) : baseShare;
            splits.add(new GroupExpenseSplit(expense, participants.get(index), share));
        }
        splitRepository.saveAll(splits);
        return GroupExpenseResponse.from(expense, splits);
    }

    @Transactional(readOnly = true)
    public List<GroupExpenseResponse> expenses(UUID userId, UUID groupId) {
        requireActiveGroup(userId, groupId);
        return expenseRepository.findAllByLedgerGroupIdOrderByOccurredAtDescIdDesc(groupId)
                .stream().map(expense -> GroupExpenseResponse.from(expense,
                        splitRepository.findAllByExpenseIdOrderByUserDisplayNameAsc(expense.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public GroupExpenseResponse expense(UUID userId, UUID groupId, UUID expenseId) {
        requireActiveGroup(userId, groupId);
        GroupExpense expense = expenseRepository.findByIdAndLedgerGroupId(expenseId, groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));
        return GroupExpenseResponse.from(expense,
                splitRepository.findAllByExpenseIdOrderByUserDisplayNameAsc(expense.getId()));
    }

    @Transactional(readOnly = true)
    public List<GroupBalanceResponse> balances(UUID userId, UUID groupId) {
        requireActiveGroup(userId, groupId);
        Map<UUID, GroupBalanceTotal> totals = memberRepository
                .findAllByLedgerGroupIdAndStatusOrderByUserDisplayNameAsc(groupId, GroupMemberStatus.ACTIVE)
                .stream().collect(Collectors.toMap(member -> member.getUser().getId(),
                        member -> new GroupBalanceTotal(member.getUser().getDisplayName()),
                        (left, right) -> left, TreeMap::new));

        for (GroupExpense expense : expenseRepository.findAllByLedgerGroupIdOrderByOccurredAtDescIdDesc(groupId)) {
            GroupBalanceTotal payerTotal = totals.get(expense.getPaidBy().getId());
            if (payerTotal != null) payerTotal.amount = payerTotal.amount.add(expense.getAmount());
        }
        for (GroupExpenseSplit split : splitRepository.findAllByExpenseLedgerGroupId(groupId)) {
            GroupBalanceTotal participantTotal = totals.get(split.getUser().getId());
            if (participantTotal != null) participantTotal.amount = participantTotal.amount.subtract(split.getShareAmount());
        }
        return totals.entrySet().stream().map(entry -> new GroupBalanceResponse(entry.getKey(),
                entry.getValue().displayName, entry.getValue().amount)).toList();
    }

    private List<AppUser> resolveParticipants(UUID groupId, List<UUID> requestedParticipantIds) {
        List<UUID> participantIds = requestedParticipantIds == null
                ? memberRepository.findAllByLedgerGroupIdAndStatusOrderByUserDisplayNameAsc(
                        groupId, GroupMemberStatus.ACTIVE).stream().map(member -> member.getUser().getId()).toList()
                : requestedParticipantIds;
        if (participantIds.isEmpty() || participantIds.stream().distinct().count() != participantIds.size()) {
            throw new GroupConflictException("Choose one or more distinct active group members to split this expense");
        }
        List<UUID> sortedIds = participantIds.stream().sorted().toList();
        List<AppUser> participants = userRepository.findAllById(sortedIds).stream()
                .sorted(java.util.Comparator.comparing(AppUser::getId)).toList();
        if (participants.size() != sortedIds.size() || participants.stream().anyMatch(participant ->
                !memberRepository.existsByLedgerGroupIdAndUserIdAndStatus(
                        groupId, participant.getId(), GroupMemberStatus.ACTIVE))) {
            throw new GroupConflictException("All expense participants must be active members of this group");
        }
        return participants;
    }

    private static final class GroupBalanceTotal {
        private final String displayName;
        private BigDecimal amount = BigDecimal.ZERO.setScale(2);

        private GroupBalanceTotal(String displayName) { this.displayName = displayName; }
    }

    @Transactional
    public GroupDisputeResponse raiseDispute(UUID userId, UUID groupId, RaiseGroupDisputeRequest request) {
        LedgerGroup group = requireActiveGroup(userId, groupId);
        GroupExpense expense = null;
        if (request.issueType() == GroupDisputeType.WRONGLY_ADDED) {
            if (request.groupExpenseId() != null) {
                throw new InvalidGroupDisputeException("Wrongly added disputes cannot target an expense");
            }
        } else if (request.issueType() == GroupDisputeType.INCORRECT_AMOUNT) {
            if (request.groupExpenseId() == null) {
                throw new InvalidGroupDisputeException("Choose the group expense with the incorrect amount");
            }
            expense = expenseRepository.findByIdAndLedgerGroupId(request.groupExpenseId(), groupId)
                    .orElseThrow(() -> new GroupNotFoundException(groupId));
        }
        AppUser raisedBy = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationUserNotFoundException(userId));
        return GroupDisputeResponse.from(disputeRepository.save(
                new GroupDispute(group, raisedBy, expense, request.issueType())));
    }

    @Transactional(readOnly = true)
    public List<GroupDisputeResponse> disputes(UUID userId, UUID groupId) {
        LedgerGroup group = requireActiveGroup(userId, groupId);
        List<GroupDispute> disputes = group.getOwner().getId().equals(userId)
                ? disputeRepository.findAllByLedgerGroupIdOrderByCreatedAtDescIdDesc(groupId)
                : disputeRepository.findAllByLedgerGroupIdAndRaisedByIdOrderByCreatedAtDescIdDesc(groupId, userId);
        return disputes.stream().map(GroupDisputeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public GroupDisputeResponse dispute(UUID userId, UUID groupId, UUID disputeId) {
        LedgerGroup group = requireActiveGroup(userId, groupId);
        GroupDispute dispute = disputeRepository.findByIdAndLedgerGroupId(disputeId, groupId)
                .orElseThrow(() -> new GroupDisputeNotFoundException(disputeId));
        if (!group.getOwner().getId().equals(userId) && !dispute.getRaisedBy().getId().equals(userId)) {
            throw new GroupDisputeNotFoundException(disputeId);
        }
        return GroupDisputeResponse.from(dispute);
    }

    @Transactional
    public GroupDisputeResponse resolveDispute(UUID ownerId, UUID groupId, UUID disputeId) {
        LedgerGroup group = requireOwnerGroup(ownerId, groupId);
        GroupDispute dispute = disputeRepository.findByIdAndLedgerGroupId(disputeId, groupId)
                .orElseThrow(() -> new GroupDisputeNotFoundException(disputeId));
        if (dispute.getStatus() != GroupDisputeStatus.OPEN) {
            throw new GroupConflictException("This dispute has already been resolved");
        }
        dispute.resolve(group.getOwner());
        return GroupDisputeResponse.from(dispute);
    }

    private GroupResponse response(LedgerGroup group) {
        List<GroupMemberResponse> members = memberRepository
                .findAllByLedgerGroupIdAndStatusOrderByUserDisplayNameAsc(group.getId(), GroupMemberStatus.ACTIVE)
                .stream().map(GroupMemberResponse::from).toList();
        return new GroupResponse(group.getId(), group.getName(), group.getCreatedAt(), members);
    }

    private LedgerGroup requireActiveGroup(UUID userId, UUID groupId) {
        LedgerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));
        if (!memberRepository.existsByLedgerGroupIdAndUserIdAndStatus(
                groupId, userId, GroupMemberStatus.ACTIVE)) {
            throw new GroupNotFoundException(groupId);
        }
        return group;
    }

    private LedgerGroup requireOwnerGroup(UUID ownerId, UUID groupId) {
        LedgerGroup group = groupRepository.findById(groupId)
                .filter(candidate -> candidate.getOwner().getId().equals(ownerId))
                .orElseThrow(() -> new GroupNotFoundException(groupId));
        return group;
    }

}
