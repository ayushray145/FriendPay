package com.splitledger.friend;

import com.splitledger.security.ApplicationOidcUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/friends")
public class FriendController {
    private final FriendService friendService;

    public FriendController(FriendService friendService) { this.friendService = friendService; }

    @GetMapping
    public List<FriendResponse> friends(@AuthenticationPrincipal ApplicationOidcUser user) {
        return friendService.friends(user.getApplicationUserId());
    }

    @GetMapping("/requests")
    public List<FriendRequestResponse> requests(@AuthenticationPrincipal ApplicationOidcUser user) {
        return friendService.requests(user.getApplicationUserId());
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public FriendRequestResponse send(@Valid @RequestBody CreateFriendRequest request,
                                      @AuthenticationPrincipal ApplicationOidcUser user) {
        return friendService.send(user.getApplicationUserId(), request);
    }

    @PostMapping("/requests/{requestId}/accept")
    public FriendResponse accept(@PathVariable UUID requestId,
                                 @AuthenticationPrincipal ApplicationOidcUser user) {
        return friendService.accept(user.getApplicationUserId(), requestId);
    }

    @PostMapping("/requests/{requestId}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decline(@PathVariable UUID requestId,
                        @AuthenticationPrincipal ApplicationOidcUser user) {
        friendService.decline(user.getApplicationUserId(), requestId);
    }

    @PatchMapping("/{requestId}/nickname")
    public FriendResponse nickname(@PathVariable UUID requestId,
                                   @Valid @RequestBody UpdateFriendNicknameRequest request,
                                   @AuthenticationPrincipal ApplicationOidcUser user) {
        return friendService.updateNickname(user.getApplicationUserId(), requestId, request);
    }
}
