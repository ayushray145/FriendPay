package com.splitledger.group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupExpenseRepository extends JpaRepository<GroupExpense, UUID> {

    List<GroupExpense> findAllByLedgerGroupIdOrderByOccurredAtDescIdDesc(UUID groupId);

    Optional<GroupExpense> findByIdAndLedgerGroupId(UUID expenseId, UUID groupId);

    @Modifying
    @Query("delete from GroupExpense expense where expense.ledgerGroup.id = :groupId")
    int deleteAllByLedgerGroupId(@Param("groupId") UUID groupId);
}
