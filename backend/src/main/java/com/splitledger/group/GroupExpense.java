package com.splitledger.group;

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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "group_expenses")
public class GroupExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private LedgerGroup ledgerGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by_user_id", nullable = false)
    private AppUser paidBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_user_id", nullable = false)
    private AppUser recordedBy;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected GroupExpense() { }

    public GroupExpense(LedgerGroup ledgerGroup, AppUser paidBy, AppUser recordedBy,
                        BigDecimal amount, String description, Instant occurredAt) {
        this.ledgerGroup = ledgerGroup;
        this.paidBy = paidBy;
        this.recordedBy = recordedBy;
        this.amount = amount;
        this.description = description;
        this.occurredAt = occurredAt;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (occurredAt == null) occurredAt = createdAt;
    }

    public UUID getId() { return id; }
    public LedgerGroup getLedgerGroup() { return ledgerGroup; }
    public AppUser getPaidBy() { return paidBy; }
    public AppUser getRecordedBy() { return recordedBy; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getCreatedAt() { return createdAt; }
}
