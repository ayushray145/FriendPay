package com.splitledger.group;

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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "group_disputes")
public class GroupDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private LedgerGroup ledgerGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raised_by_user_id", nullable = false)
    private AppUser raisedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_expense_id")
    private GroupExpense groupExpense;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispute_type", nullable = false, length = 32)
    private GroupDisputeType issueType;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispute_status", nullable = false, length = 16)
    private GroupDisputeStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private AppUser resolvedBy;

    protected GroupDispute() { }

    public GroupDispute(LedgerGroup ledgerGroup, AppUser raisedBy, GroupExpense groupExpense,
                        GroupDisputeType issueType) {
        this.ledgerGroup = ledgerGroup;
        this.raisedBy = raisedBy;
        this.groupExpense = groupExpense;
        this.issueType = issueType;
        this.status = GroupDisputeStatus.OPEN;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public void resolve(AppUser resolvedBy) {
        if (status != GroupDisputeStatus.OPEN) {
            throw new IllegalStateException("Only open disputes can be resolved");
        }
        this.status = GroupDisputeStatus.RESOLVED;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public LedgerGroup getLedgerGroup() { return ledgerGroup; }
    public AppUser getRaisedBy() { return raisedBy; }
    public GroupExpense getGroupExpense() { return groupExpense; }
    public GroupDisputeType getIssueType() { return issueType; }
    public GroupDisputeStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public AppUser getResolvedBy() { return resolvedBy; }
}
