package com.splitledger.group;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupExpenseSplitRepository extends JpaRepository<GroupExpenseSplit, UUID> {

    List<GroupExpenseSplit> findAllByExpenseIdOrderByUserDisplayNameAsc(UUID expenseId);

    List<GroupExpenseSplit> findAllByExpenseLedgerGroupId(UUID groupId);
}
