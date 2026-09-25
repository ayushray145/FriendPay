package com.splitledger.settlement;

import com.splitledger.security.ApplicationOidcUser;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/people/{personId}")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/settlements")
    public ResponseEntity<SettlementResponse> create(
            @PathVariable UUID personId,
            @Valid @RequestBody CreateSettlementRequest request,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        SettlementResponse settlement = settlementService.create(user.getApplicationUserId(), personId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{settlementId}")
                .buildAndExpand(settlement.id()).toUri();
        return ResponseEntity.created(location).body(settlement);
    }

    @GetMapping("/settlements")
    public List<SettlementResponse> history(
            @PathVariable UUID personId,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        return settlementService.history(user.getApplicationUserId(), personId);
    }

    @GetMapping("/ledger")
    public List<PersonLedgerEntryResponse> ledger(
            @PathVariable UUID personId,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        return settlementService.ledger(user.getApplicationUserId(), personId);
    }
}
