package com.splitledger.expense;

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
@Table(name = "expenses")
public class Expense {

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
    @Column(name = "debt_direction", nullable = false, length = 32)
    private DebtDirection debtDirection;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Version
    private long version;

    protected Expense() {
    }

    public Expense(AppUser owner, Person person, BigDecimal amount, DebtDirection debtDirection,
                   String description, Instant occurredAt) {
        this.owner = owner;
        this.person = person;
        this.amount = amount;
        this.debtDirection = debtDirection;
        this.description = description;
        this.occurredAt = occurredAt;
    }

    @PrePersist
    void onCreate() { if (occurredAt == null) occurredAt = Instant.now(); }

    public UUID getId() { return id; }
    public AppUser getOwner() { return owner; }
    public Person getPerson() { return person; }
    public BigDecimal getAmount() { return amount; }
    public DebtDirection getDebtDirection() { return debtDirection; }
    public String getDescription() { return description; }
    public Instant getOccurredAt() { return occurredAt; }
    public long getVersion() { return version; }
}
