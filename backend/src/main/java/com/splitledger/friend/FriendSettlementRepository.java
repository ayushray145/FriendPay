package com.splitledger.friend;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendSettlementRepository extends JpaRepository<FriendSettlement, UUID> {

    @Query("""
            select coalesce(sum(settlement.amount), 0)
            from FriendSettlement settlement
            where settlement.payer.id = :payerId and settlement.recipient.id = :recipientId
            """)
    BigDecimal sumByPayerAndRecipient(@Param("payerId") UUID payerId,
                                      @Param("recipientId") UUID recipientId);

    @Query("""
            select settlement from FriendSettlement settlement
            where (settlement.payer.id = :firstUserId and settlement.recipient.id = :secondUserId)
               or (settlement.payer.id = :secondUserId and settlement.recipient.id = :firstUserId)
            order by settlement.settledAt desc, settlement.id desc
            """)
    List<FriendSettlement> findAllBetweenUsers(@Param("firstUserId") UUID firstUserId,
                                               @Param("secondUserId") UUID secondUserId);

    @Query("""
            select settlement.payer.id as payerUserId,
                   settlement.recipient.id as recipientUserId,
                   sum(settlement.amount) as amount
            from FriendSettlement settlement
            where settlement.payer.id = :ownerId or settlement.recipient.id = :ownerId
            group by settlement.payer.id, settlement.recipient.id
            """)
    List<FriendSettlementBalanceProjection> findBalanceAdjustmentsByOwnerId(@Param("ownerId") UUID ownerId);
}
