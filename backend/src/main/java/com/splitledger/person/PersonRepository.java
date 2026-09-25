package com.splitledger.person;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonRepository extends JpaRepository<Person, UUID> {

    List<Person> findAllByOwnerIdOrderByDisplayNameAsc(UUID ownerId);

    Optional<Person> findByIdAndOwnerId(UUID id, UUID ownerId);

    Optional<Person> findByOwnerIdAndLinkedUserId(UUID ownerId, UUID linkedUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select person from Person person where person.id = :personId and person.owner.id = :ownerId")
    Optional<Person> findOwnedPersonForUpdate(@Param("personId") UUID personId, @Param("ownerId") UUID ownerId);
}
