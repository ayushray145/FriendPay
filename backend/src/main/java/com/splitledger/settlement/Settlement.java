package com.splitledger.settlement;

import com.splitledger.ledger.DebtDirection;
import com.splitledger.person.Person;
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
@Table(name = "settlements")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private AppUser owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_direction", nullable = false, length = 32)
    private DebtDirection paymentDirection;

    @Column(name = "settled_at", nullable = false)
    private Instant settledAt;

    @Version
    private long version;

    protected Settlement() {
    }

    public Settlement(AppUser owner, Person person, BigDecimal amount, DebtDirection paymentDirection,
                      Instant settledAt) {
        this.owner = owner;
        this.person = person;
        this.amount = amount;
        this.paymentDirection = paymentDirection;
        this.settledAt = settledAt;
    }

    @PrePersist
    void onCreate() { if (settledAt == null) settledAt = Instant.now(); }

    public UUID getId() { return id; }
    public AppUser getOwner() { return owner; }
    public Person getPerson() { return person; }
    public BigDecimal getAmount() { return amount; }
    public DebtDirection getPaymentDirection() { return paymentDirection; }
    public Instant getSettledAt() { return settledAt; }
    public long getVersion() { return version; }
}
