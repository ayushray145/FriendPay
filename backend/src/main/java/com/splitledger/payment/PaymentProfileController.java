package com.splitledger.payment;

import com.splitledger.security.ApplicationOidcUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/payment-profile")
public class PaymentProfileController {

    private final PaymentProfileService paymentProfileService;

    public PaymentProfileController(PaymentProfileService paymentProfileService) {
        this.paymentProfileService = paymentProfileService;
    }

    @GetMapping
    public PaymentProfileResponse get(@AuthenticationPrincipal ApplicationOidcUser user) {
        return paymentProfileService.get(user.getApplicationUserId());
    }

    @PutMapping
    public PaymentProfileResponse save(
            @Valid @RequestBody SavePaymentProfileRequest request,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        return paymentProfileService.save(user.getApplicationUserId(), request);
    }

    @PutMapping("/sharing")
    public PaymentProfileResponse sharing(
            @Valid @RequestBody PaymentProfileSharingRequest request,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        return paymentProfileService.updateFriendSharing(user.getApplicationUserId(), request);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthenticationPrincipal ApplicationOidcUser user) {
        paymentProfileService.delete(user.getApplicationUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/upi-link")
    public PaymentLinkResponse paymentLink(
            @RequestParam @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        return new PaymentLinkResponse(paymentProfileService.paymentUri(user.getApplicationUserId(), amount, note));
    }

    @GetMapping("/friends/{friendId}/upi-link")
    public PaymentLinkResponse friendPaymentLink(
            @PathVariable UUID friendId,
            @RequestParam @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        return paymentProfileService.friendPaymentUri(user.getApplicationUserId(), friendId, amount, note);
    }

    @GetMapping(value = "/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(
            @RequestParam @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        byte[] image = paymentProfileService.qrCode(user.getApplicationUserId(), amount, note);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).body(image);
    }

    @GetMapping(value = "/my-qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> personalQr(@AuthenticationPrincipal ApplicationOidcUser user) {
        byte[] image = paymentProfileService.personalQrCode(user.getApplicationUserId());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).body(image);
    }
}
