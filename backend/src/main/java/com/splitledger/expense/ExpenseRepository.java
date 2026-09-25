package com.splitledger.expense;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.splitledger.ledger.DebtDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findAllByOwnerIdAndPersonIdOrderByOccurredAtDescIdDesc(UUID ownerId, UUID personId);

    Optional<Expense> findByIdAndOwnerIdAndPersonId(UUID id, UUID ownerId, UUID personId);

    @Query("""
            select sum(expense.amount)
            from Expense expense
            where expense.owner.id = :ownerId
              and expense.person.id = :personId
              and expense.debtDirection = :direction
            """)
    BigDecimal sumAmountByOwnerAndPersonAndDirection(
            @Param("ownerId") UUID ownerId,
            @Param("personId") UUID personId,
            @Param("direction") DebtDirection direction);

    @Query("""
            select person.id as personId,
                   person.displayName as displayName,
                   sum(case when expense.debtDirection = :personOwesUser
                            then expense.amount else -expense.amount end) as netBalance
            from Expense expense
            join expense.person person
            where expense.owner.id = :ownerId
            group by person.id, person.displayName
            order by person.displayName
            """)
    List<PersonExpenseBalanceProjection> findBalancesByOwnerId(
            @Param("ownerId") UUID ownerId,
            @Param("personOwesUser") DebtDirection personOwesUser);
}
