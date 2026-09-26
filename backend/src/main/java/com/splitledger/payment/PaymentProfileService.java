package com.splitledger.payment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.splitledger.security.ApplicationUserNotFoundException;
import com.splitledger.friend.FriendRequestNotFoundException;
import com.splitledger.friend.FriendRequestRepository;
import com.splitledger.friend.FriendRequestStatus;
import com.splitledger.user.AppUser;
import com.splitledger.user.AppUserRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentProfileService {

    private static final int QR_SIZE = 320;

    private final PaymentProfileRepository profileRepository;
    private final AppUserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;

    public PaymentProfileService(PaymentProfileRepository profileRepository, AppUserRepository userRepository,
                                 FriendRequestRepository friendRequestRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.friendRequestRepository = friendRequestRepository;
    }

    @Transactional(readOnly = true)
    public PaymentProfileResponse get(UUID userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationUserNotFoundException(userId));
        return profileRepository.findByOwnerId(userId)
                .map(PaymentProfileResponse::from)
                .orElseGet(() -> new PaymentProfileResponse(userId, user.getDisplayName(), null, null, false));
    }

    @Transactional
    public PaymentProfileResponse save(UUID userId, SavePaymentProfileRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationUserNotFoundException(userId));
        String upiId = request.upiId().trim();
        PaymentProfile profile = profileRepository.findByOwnerId(userId)
                .map(existing -> {
                    existing.updateUpiId(upiId);
                    return existing;
                })
                .orElseGet(() -> new PaymentProfile(user, upiId));
        return PaymentProfileResponse.from(profileRepository.saveAndFlush(profile));
    }

    @Transactional
    public PaymentProfileResponse updateFriendSharing(UUID userId, PaymentProfileSharingRequest request) {
        PaymentProfile profile = requireProfile(userId);
        profile.setSharedWithFriends(request.sharedWithFriends());
        return PaymentProfileResponse.from(profileRepository.saveAndFlush(profile));
    }

    @Transactional
    public void delete(UUID userId) {
        profileRepository.findByOwnerId(userId).ifPresent(profileRepository::delete);
    }

    @Transactional(readOnly = true)
    public String paymentUri(UUID userId, BigDecimal amount, String note) {
        PaymentProfile profile = requireProfile(userId);
        return paymentUri(profile, amount, note);
    }

    @Transactional(readOnly = true)
    public PaymentLinkResponse friendPaymentUri(UUID userId, UUID friendId, BigDecimal amount, String note) {
        PaymentProfile profile = requireFriendSharedProfile(userId, friendId);
        return new PaymentLinkResponse(paymentUri(profile, amount, note));
    }

    @Transactional(readOnly = true)
    public byte[] friendQrCode(UUID userId, UUID friendId, BigDecimal amount, String note) {
        PaymentProfile profile = requireFriendSharedProfile(userId, friendId);
        return createQrCode(paymentUri(profile, amount, note));
    }

    private PaymentProfile requireFriendSharedProfile(UUID userId, UUID friendId) {
        friendRequestRepository.findBetweenUsersWithStatus(userId, friendId, FriendRequestStatus.ACCEPTED)
                .orElseThrow(() -> new FriendRequestNotFoundException(friendId));
        return profileRepository.findByOwnerId(friendId)
                .filter(PaymentProfile::isSharedWithFriends)
                .orElseThrow(FriendPaymentUnavailableException::new);
    }

    private String paymentUri(PaymentProfile profile, BigDecimal amount, String note) {
        String displayName = profile.getOwner().getDisplayName();
        return "upi://pay?pa=" + encode(profile.getUpiId())
                + "&pn=" + encode(displayName)
                + "&am=" + amount.setScale(2).toPlainString()
                + "&cu=INR"
                + "&tn=" + encode(normalizeNote(note));
    }

    @Transactional(readOnly = true)
    public byte[] qrCode(UUID userId, BigDecimal amount, String note) {
        String upiUri = paymentUri(userId, amount, note);
        return createQrCode(upiUri);
    }

    @Transactional(readOnly = true)
    public byte[] personalQrCode(UUID userId) {
        PaymentProfile profile = requireProfile(userId);
        String upiUri = "upi://pay?pa=" + encode(profile.getUpiId())
                + "&pn=" + encode(profile.getOwner().getDisplayName())
                + "&cu=INR";
        return createQrCode(upiUri);
    }

    private byte[] createQrCode(String upiUri) {
        try {
            var matrix = new QRCodeWriter().encode(upiUri, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException exception) {
            throw new IllegalStateException("Could not generate UPI QR code", exception);
        }
    }

    private PaymentProfile requireProfile(UUID userId) {
        return profileRepository.findByOwnerId(userId).orElseThrow(PaymentProfileNotConfiguredException::new);
    }

    private String normalizeNote(String note) {
        if (note == null || note.isBlank()) return "Split Ledger settlement";
        String trimmed = note.trim();
        return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
