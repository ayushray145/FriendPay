package com.splitledger.expense;

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
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping("/expenses")
    public ResponseEntity<ExpenseResponse> create(
            @PathVariable UUID personId,
            @Valid @RequestBody CreateExpenseRequest request,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        ExpenseResponse expense = expenseService.create(user.getApplicationUserId(), personId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{expenseId}")
                .buildAndExpand(expense.id())
                .toUri();
        return ResponseEntity.created(location).body(expense);
    }

    @GetMapping("/expenses")
    public List<ExpenseResponse> history(
            @PathVariable UUID personId,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        return expenseService.history(user.getApplicationUserId(), personId);
    }

    @GetMapping("/expenses/{expenseId}")
    public ExpenseResponse get(
            @PathVariable UUID personId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        return expenseService.get(user.getApplicationUserId(), personId, expenseId);
    }

    @GetMapping("/balance")
    public PersonBalanceResponse balance(
            @PathVariable UUID personId,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        return expenseService.balance(user.getApplicationUserId(), personId);
    }
}
