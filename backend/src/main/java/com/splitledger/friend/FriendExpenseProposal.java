package com.splitledger.friend;

import com.splitledger.ledger.DebtDirection;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "friend_expense_proposals")
public class FriendExpenseProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "friend_request_id", nullable = false)
    private FriendRequest friendRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private AppUser requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private AppUser recipient;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "debt_direction", nullable = false, length = 32)
    private DebtDirection debtDirection;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposal_status", nullable = false, length = 16)
    private FriendExpenseProposalStatus status;

    @Column(name = "dispute_reason", length = 500)
    private String disputeReason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Version
    private long version;

    protected FriendExpenseProposal() { }

    public FriendExpenseProposal(FriendRequest friendRequest, AppUser requester, AppUser recipient,
                                 BigDecimal amount, String description, DebtDirection debtDirection) {
        this.friendRequest = friendRequest;
        this.requester = requester;
        this.recipient = recipient;
        this.amount = amount;
        this.description = description;
        this.debtDirection = debtDirection;
        this.status = FriendExpenseProposalStatus.PENDING;
        this.occurredAt = Instant.now();
    }

    public void dispute(String reason) {
        if (status != FriendExpenseProposalStatus.PENDING) {
            throw new IllegalStateException("Only pending friend expenses can be disputed");
        }
        status = FriendExpenseProposalStatus.DISPUTED;
        disputeReason = reason;
    }

    public void reviseAndResubmit(BigDecimal amount, String description, DebtDirection debtDirection) {
        if (status != FriendExpenseProposalStatus.DISPUTED) {
            throw new IllegalStateException("Only disputed friend expenses can be revised");
        }
        this.amount = amount;
        this.description = description;
        this.debtDirection = debtDirection;
        this.disputeReason = null;
        this.status = FriendExpenseProposalStatus.PENDING;
    }

    public void approve() {
        if (status != FriendExpenseProposalStatus.PENDING) {
            throw new IllegalStateException("Only pending friend expenses can be approved");
        }
        status = FriendExpenseProposalStatus.APPROVED;
        approvedAt = Instant.now();
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public FriendRequest getFriendRequest() { return friendRequest; }
    public AppUser getRequester() { return requester; }
    public AppUser getRecipient() { return recipient; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public DebtDirection getDebtDirection() { return debtDirection; }
    public FriendExpenseProposalStatus getStatus() { return status; }
    public String getDisputeReason() { return disputeReason; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getApprovedAt() { return approvedAt; }
}
