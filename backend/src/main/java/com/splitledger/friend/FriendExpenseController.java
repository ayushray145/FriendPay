package com.splitledger.friend;

import com.splitledger.security.ApplicationOidcUser;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/friend-expenses")
public class FriendExpenseController {

    private final FriendExpenseService friendExpenseService;

    public FriendExpenseController(FriendExpenseService friendExpenseService) {
        this.friendExpenseService = friendExpenseService;
    }

    @PostMapping
    public ResponseEntity<FriendExpenseProposalResponse> create(
            @Valid @RequestBody CreateFriendExpenseRequest request,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        FriendExpenseProposalResponse proposal = friendExpenseService.create(user.getApplicationUserId(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{proposalId}")
                .buildAndExpand(proposal.id()).toUri();
        return ResponseEntity.created(location).body(proposal);
    }

    @GetMapping
    public List<FriendExpenseProposalResponse> list(@AuthenticationPrincipal ApplicationOidcUser user) {
        return friendExpenseService.list(user.getApplicationUserId());
    }

    @PostMapping("/{proposalId}/approve")
    public FriendExpenseProposalResponse approve(
            @PathVariable UUID proposalId, @AuthenticationPrincipal ApplicationOidcUser user) {
        return friendExpenseService.approve(user.getApplicationUserId(), proposalId);
    }

    @PostMapping("/{proposalId}/dispute")
    public FriendExpenseProposalResponse dispute(
            @PathVariable UUID proposalId,
            @Valid @RequestBody DisputeFriendExpenseRequest request,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        return friendExpenseService.dispute(user.getApplicationUserId(), proposalId, request);
    }

    @PatchMapping("/{proposalId}")
    public FriendExpenseProposalResponse reviseAndResubmit(
            @PathVariable UUID proposalId,
            @Valid @RequestBody ReviseFriendExpenseRequest request,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        return friendExpenseService.reviseAndResubmit(user.getApplicationUserId(), proposalId, request);
    }
}
