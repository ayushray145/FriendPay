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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "group_expense_splits", uniqueConstraints = @UniqueConstraint(
        name = "uk_group_expense_splits_expense_user", columnNames = {"group_expense_id", "user_id"}))
public class GroupExpenseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_expense_id", nullable = false)
    private GroupExpense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "share_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal shareAmount;

    protected GroupExpenseSplit() { }

    public GroupExpenseSplit(GroupExpense expense, AppUser user, BigDecimal shareAmount) {
        this.expense = expense;
        this.user = user;
        this.shareAmount = shareAmount;
    }

    public GroupExpense getExpense() { return expense; }
    public AppUser getUser() { return user; }
    public BigDecimal getShareAmount() { return shareAmount; }
}
