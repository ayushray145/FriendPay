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
@Table(name = "group_members")
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private LedgerGroup ledgerGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_user_id")
    private AppUser addedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_role", nullable = false, length = 16)
    private GroupMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false, length = 16)
    private GroupMemberStatus status;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    @Column(name = "joined_at")
    private Instant joinedAt;

    protected GroupMember() { }

    public GroupMember(LedgerGroup ledgerGroup, AppUser user, AppUser addedBy,
                       GroupMemberRole role, GroupMemberStatus status) {
        this.ledgerGroup = ledgerGroup;
        this.user = user;
        this.addedBy = addedBy;
        this.role = role;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        addedAt = Instant.now();
        if (status == GroupMemberStatus.ACTIVE && joinedAt == null) {
            joinedAt = addedAt;
        }
    }

    public void remove() {
        status = GroupMemberStatus.REMOVED;
        joinedAt = null;
    }

    public void addAgain(AppUser addedBy) {
        if (status != GroupMemberStatus.REMOVED || role == GroupMemberRole.OWNER) {
            throw new IllegalStateException("Only removed members can be added again");
        }
        this.addedBy = addedBy;
        this.status = GroupMemberStatus.ACTIVE;
        this.addedAt = Instant.now();
        this.joinedAt = this.addedAt;
    }

    public UUID getId() { return id; }
    public LedgerGroup getLedgerGroup() { return ledgerGroup; }
    public AppUser getUser() { return user; }
    public AppUser getAddedBy() { return addedBy; }
    public GroupMemberRole getRole() { return role; }
    public GroupMemberStatus getStatus() { return status; }
    public Instant getAddedAt() { return addedAt; }
    public Instant getJoinedAt() { return joinedAt; }
}
