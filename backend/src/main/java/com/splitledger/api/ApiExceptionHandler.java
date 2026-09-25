package com.splitledger.api;

import com.splitledger.person.PersonNotFoundException;
import com.splitledger.security.ApplicationUserNotFoundException;
import com.splitledger.expense.ExpenseNotFoundException;
import com.splitledger.friend.FriendAccountNotFoundException;
import com.splitledger.friend.FriendConflictException;
import com.splitledger.friend.FriendRequestNotFoundException;
import com.splitledger.group.GroupConflictException;
import com.splitledger.group.GroupAccountNotFoundException;
import com.splitledger.group.GroupDisputeNotFoundException;
import com.splitledger.group.InvalidGroupDisputeException;
import com.splitledger.group.GroupNotFoundException;
import com.splitledger.payment.PaymentProfileNotConfiguredException;
import com.splitledger.settlement.SettlementExceedsOutstandingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PersonNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> personNotFound(
            PersonNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Person not found", request);
    }

    @ExceptionHandler(ExpenseNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> expenseNotFound(
            ExpenseNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Expense not found", request);
    }

    @ExceptionHandler(FriendRequestNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> friendRequestNotFound(
            FriendRequestNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Friend request or friendship not found", request);
    }

    @ExceptionHandler(FriendAccountNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> friendAccountNotFound(
            FriendAccountNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(FriendConflictException.class)
    public ResponseEntity<ApiErrorResponse> friendConflict(
            FriendConflictException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(SettlementExceedsOutstandingException.class)
    public ResponseEntity<ApiErrorResponse> settlementExceedsOutstanding(
            SettlementExceedsOutstandingException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler({GroupNotFoundException.class, GroupDisputeNotFoundException.class, GroupAccountNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> groupNotFound(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Group or member not found", request);
    }

    @ExceptionHandler(GroupConflictException.class)
    public ResponseEntity<ApiErrorResponse> groupConflict(
            GroupConflictException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidGroupDisputeException.class)
    public ResponseEntity<ApiErrorResponse> invalidGroupDispute(
            InvalidGroupDisputeException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(PaymentProfileNotConfiguredException.class)
    public ResponseEntity<ApiErrorResponse> paymentProfileNotConfigured(
            PaymentProfileNotConfiguredException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(ApplicationUserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> applicationUserNotFound(
            ApplicationUserNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "Authenticated account is unavailable", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> invalidRequest(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("Request validation failed");
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> unreadableRequest(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Request body is invalid", request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> invalidParameter(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Request parameter is invalid", request);
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message, HttpServletRequest request) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(response);
    }
}
