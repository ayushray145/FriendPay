package com.splitledger.person;

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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "people")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private AppUser owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_user_id")
    private AppUser linkedUser;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "phone_number", length = 16)
    private String phoneNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Person() {
    }

    public Person(AppUser owner, AppUser linkedUser, String displayName) {
        this(owner, linkedUser, displayName, null);
    }

    public Person(AppUser owner, AppUser linkedUser, String displayName, String phoneNumber) {
        this.owner = owner;
        this.linkedUser = linkedUser;
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public AppUser getOwner() { return owner; }
    public AppUser getLinkedUser() { return linkedUser; }
    public String getDisplayName() { return displayName; }
    public void rename(String displayName) { this.displayName = displayName; }
    public String getPhoneNumber() { return phoneNumber; }
    public Instant getCreatedAt() { return createdAt; }
}
