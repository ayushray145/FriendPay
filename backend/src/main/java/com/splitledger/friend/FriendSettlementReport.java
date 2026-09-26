package com.splitledger.friend;

import com.splitledger.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "friend_settlement_reports")
public class FriendSettlementReport {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "friend_request_id", nullable = false)
    private FriendRequest friendRequest;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_user_id", nullable = false)
    private AppUser payer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private AppUser recipient;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(name = "payment_reference", length = 80)
    private String paymentReference;
    @Enumerated(EnumType.STRING)
    @Column(name = "report_status", nullable = false, length = 16)
    private FriendSettlementReportStatus status;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    @Version
    private long version;

    protected FriendSettlementReport() { }
    public FriendSettlementReport(FriendRequest friendRequest, AppUser payer, AppUser recipient,
                                  BigDecimal amount, String paymentReference) {
        this.friendRequest = friendRequest;
        this.payer = payer;
        this.recipient = recipient;
        this.amount = amount;
        this.paymentReference = paymentReference == null || paymentReference.isBlank() ? null : paymentReference.trim();
        this.status = FriendSettlementReportStatus.PENDING;
    }
    public void approve() { review(FriendSettlementReportStatus.APPROVED); }
    public void reject() { review(FriendSettlementReportStatus.REJECTED); }
    private void review(FriendSettlementReportStatus nextStatus) {
        if (status != FriendSettlementReportStatus.PENDING) throw new IllegalStateException("Only pending reports can be reviewed");
        status = nextStatus;
        reviewedAt = Instant.now();
    }
    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public FriendRequest getFriendRequest() { return friendRequest; }
    public AppUser getPayer() { return payer; }
    public AppUser getRecipient() { return recipient; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentReference() { return paymentReference; }
    public FriendSettlementReportStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
}
