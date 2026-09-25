package com.splitledger.group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerGroupRepository extends JpaRepository<LedgerGroup, UUID> {

    @Query("""
            select distinct ledgerGroup
            from LedgerGroup ledgerGroup
            left join GroupMember membership on membership.ledgerGroup = ledgerGroup
            where ledgerGroup.owner.id = :userId
               or (membership.user.id = :userId and membership.status = :active)
            order by ledgerGroup.name
            """)
    List<LedgerGroup> findVisibleToUser(@Param("userId") UUID userId,
                                        @Param("active") GroupMemberStatus active);

    Optional<LedgerGroup> findById(UUID id);
}
