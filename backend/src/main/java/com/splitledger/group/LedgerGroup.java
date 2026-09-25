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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_groups")
public class LedgerGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private AppUser owner;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected LedgerGroup() { }

    public LedgerGroup(AppUser owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public AppUser getOwner() { return owner; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
}
