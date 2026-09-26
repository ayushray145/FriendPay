package com.splitledger.group;

import com.splitledger.security.ApplicationOidcUser;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupResponse> create(
            @Valid @RequestBody CreateGroupRequest request,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        GroupResponse group = groupService.create(user.getApplicationUserId(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{groupId}")
                .buildAndExpand(group.id()).toUri();
        return ResponseEntity.created(location).body(group);
    }

    @GetMapping
    public List<GroupResponse> list(@AuthenticationPrincipal ApplicationOidcUser user) {
        return groupService.list(user.getApplicationUserId());
    }

    @GetMapping("/{groupId}")
    public GroupResponse get(@PathVariable UUID groupId, @AuthenticationPrincipal ApplicationOidcUser user) {
        return groupService.get(user.getApplicationUserId(), groupId);
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        groupService.delete(user.getApplicationUserId(), groupId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/delete")
    public ResponseEntity<Void> deleteWithPost(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        groupService.delete(user.getApplicationUserId(), groupId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupMemberResponse> addMember(
            @PathVariable UUID groupId,
            @Valid @RequestBody AddGroupMemberRequest request,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        GroupMemberResponse member = groupService.addMember(user.getApplicationUserId(), groupId, request);
        return ResponseEntity.ok(member);
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID groupId,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        groupService.removeMember(user.getApplicationUserId(), groupId, memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/expenses")
    public ResponseEntity<GroupExpenseResponse> createExpense(
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateGroupExpenseRequest request,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        GroupExpenseResponse expense = groupService.createExpense(user.getApplicationUserId(), groupId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{expenseId}")
                .buildAndExpand(expense.id()).toUri();
        return ResponseEntity.created(location).body(expense);
    }

    @GetMapping("/{groupId}/expenses")
    public List<GroupExpenseResponse> expenses(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        return groupService.expenses(user.getApplicationUserId(), groupId);
    }

    @GetMapping("/{groupId}/balances")
    public List<GroupBalanceResponse> balances(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        return groupService.balances(user.getApplicationUserId(), groupId);
    }

    @GetMapping("/{groupId}/expenses/{expenseId}")
    public GroupExpenseResponse expense(
            @PathVariable UUID groupId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        return groupService.expense(user.getApplicationUserId(), groupId, expenseId);
    }

    @PostMapping("/{groupId}/disputes")
    public ResponseEntity<GroupDisputeResponse> raiseDispute(
            @PathVariable UUID groupId,
            @Valid @RequestBody RaiseGroupDisputeRequest request,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        GroupDisputeResponse dispute = groupService.raiseDispute(user.getApplicationUserId(), groupId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{disputeId}")
                .buildAndExpand(dispute.id()).toUri();
        return ResponseEntity.created(location).body(dispute);
    }

    @GetMapping("/{groupId}/disputes")
    public List<GroupDisputeResponse> disputes(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        return groupService.disputes(user.getApplicationUserId(), groupId);
    }

    @GetMapping("/{groupId}/disputes/{disputeId}")
    public GroupDisputeResponse dispute(
            @PathVariable UUID groupId,
            @PathVariable UUID disputeId,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        return groupService.dispute(user.getApplicationUserId(), groupId, disputeId);
    }

    @PostMapping("/{groupId}/disputes/{disputeId}/resolve")
    public GroupDisputeResponse resolveDispute(
            @PathVariable UUID groupId,
            @PathVariable UUID disputeId,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        return groupService.resolveDispute(user.getApplicationUserId(), groupId, disputeId);
    }
}
