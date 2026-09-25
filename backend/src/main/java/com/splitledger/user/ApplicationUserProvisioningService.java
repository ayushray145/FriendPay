package com.splitledger.user;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationUserProvisioningService {

    private static final String GOOGLE_PROVIDER = "google";

    private final AppUserRepository appUserRepository;

    public ApplicationUserProvisioningService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public AppUser findOrCreateGoogleUser(String subject, String email, String displayName) {
        Optional<AppUser> existing = appUserRepository
                .findByAuthProviderAndAuthProviderSubject(GOOGLE_PROVIDER, subject);
        if (existing.isPresent()) {
            return existing.get();
        }

        return appUserRepository.save(new AppUser(GOOGLE_PROVIDER, subject, email, displayName));
    }
}
