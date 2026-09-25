package com.splitledger.payment;

import com.splitledger.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_profiles_owner", columnNames = "owner_user_id"),
        @UniqueConstraint(name = "uk_payment_profiles_upi_id", columnNames = "upi_id")
})
public class PaymentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private AppUser owner;

    @Column(name = "upi_id", nullable = false, length = 320)
    private String upiId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentProfile() { }

    public PaymentProfile(AppUser owner, String upiId) {
        this.owner = owner;
        this.upiId = upiId;
    }

    public void updateUpiId(String upiId) { this.upiId = upiId; }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public AppUser getOwner() { return owner; }
    public String getUpiId() { return upiId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
