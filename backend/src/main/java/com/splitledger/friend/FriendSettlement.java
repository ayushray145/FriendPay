package com.splitledger.friend;

import com.splitledger.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "friend_settlements")
public class FriendSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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

    @Column(name = "settled_at", nullable = false)
    private Instant settledAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private AppUser createdBy;

    protected FriendSettlement() { }

    public FriendSettlement(FriendRequest friendRequest, AppUser payer, AppUser recipient,
                            BigDecimal amount, Instant settledAt, AppUser createdBy) {
        this.friendRequest = friendRequest;
        this.payer = payer;
        this.recipient = recipient;
        this.amount = amount;
        this.settledAt = settledAt;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() { if (settledAt == null) settledAt = Instant.now(); }

    public UUID getId() { return id; }
    public FriendRequest getFriendRequest() { return friendRequest; }
    public AppUser getPayer() { return payer; }
    public AppUser getRecipient() { return recipient; }
    public BigDecimal getAmount() { return amount; }
    public Instant getSettledAt() { return settledAt; }
}
