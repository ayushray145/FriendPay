package com.splitledger.group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupExpenseRepository extends JpaRepository<GroupExpense, UUID> {

    List<GroupExpense> findAllByLedgerGroupIdOrderByOccurredAtDescIdDesc(UUID groupId);

    Optional<GroupExpense> findByIdAndLedgerGroupId(UUID expenseId, UUID groupId);
}
