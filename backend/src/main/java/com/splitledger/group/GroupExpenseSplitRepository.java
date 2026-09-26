package com.splitledger.group;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupExpenseSplitRepository extends JpaRepository<GroupExpenseSplit, UUID> {

    List<GroupExpenseSplit> findAllByExpenseIdOrderByUserDisplayNameAsc(UUID expenseId);

    List<GroupExpenseSplit> findAllByExpenseLedgerGroupId(UUID groupId);

    @Modifying
    @Query("delete from GroupExpenseSplit split where split.expense.ledgerGroup.id = :groupId")
    int deleteAllByLedgerGroupId(@Param("groupId") UUID groupId);
}
