package com.splitledger.payment;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentProfileRepository extends JpaRepository<PaymentProfile, UUID> {

    Optional<PaymentProfile> findByOwnerId(UUID ownerId);
}
