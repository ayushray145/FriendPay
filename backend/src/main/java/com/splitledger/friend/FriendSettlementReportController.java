package com.splitledger.friend;

import com.splitledger.security.ApplicationOidcUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/friend-settlement-reports")
public class FriendSettlementReportController {
    private final FriendSettlementReportService service;
    public FriendSettlementReportController(FriendSettlementReportService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FriendSettlementReportResponse create(@Valid @RequestBody CreateFriendSettlementReportRequest request,
                                                 @AuthenticationPrincipal ApplicationOidcUser user) {
        return service.create(user.getApplicationUserId(), request);
    }

    @GetMapping
    public List<FriendSettlementReportResponse> list(@AuthenticationPrincipal ApplicationOidcUser user) {
        return service.list(user.getApplicationUserId());
    }

    @PostMapping("/{reportId}/approve")
    public FriendSettlementReportResponse approve(@PathVariable UUID reportId,
                                                 @AuthenticationPrincipal ApplicationOidcUser user) {
        return service.approve(user.getApplicationUserId(), reportId);
    }

    @PostMapping("/{reportId}/reject")
    public FriendSettlementReportResponse reject(@PathVariable UUID reportId,
                                                @AuthenticationPrincipal ApplicationOidcUser user) {
        return service.reject(user.getApplicationUserId(), reportId);
    }
}
