package com.splitledger.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByAuthProviderAndAuthProviderSubject(String authProvider, String authProviderSubject);

    Optional<AppUser> findByEmailIgnoreCase(String email);
}
