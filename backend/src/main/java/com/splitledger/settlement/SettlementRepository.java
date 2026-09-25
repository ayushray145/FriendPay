package com.splitledger.settlement;

import com.splitledger.ledger.DebtDirection;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    List<Settlement> findAllByOwnerIdAndPersonIdOrderBySettledAtDescIdDesc(UUID ownerId, UUID personId);

    @Query("""
            select sum(settlement.amount)
            from Settlement settlement
            where settlement.owner.id = :ownerId
              and settlement.person.id = :personId
              and settlement.paymentDirection = :direction
            """)
    BigDecimal sumAmountByOwnerAndPersonAndDirection(
            @Param("ownerId") UUID ownerId,
            @Param("personId") UUID personId,
            @Param("direction") DebtDirection direction);

    @Query("""
            select settlement.person.id as personId,
                   sum(case when settlement.paymentDirection = :personOwesUser
                            then -settlement.amount else settlement.amount end) as balanceAdjustment
            from Settlement settlement
            where settlement.owner.id = :ownerId
            group by settlement.person.id
            """)
    List<PersonSettlementBalanceProjection> findBalanceAdjustmentsByOwnerId(
            @Param("ownerId") UUID ownerId,
            @Param("personOwesUser") DebtDirection personOwesUser);
}
