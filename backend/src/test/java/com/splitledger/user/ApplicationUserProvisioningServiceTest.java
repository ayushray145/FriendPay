package com.splitledger.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationUserProvisioningServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private ApplicationUserProvisioningService service;

    @Test
    void existingProviderSubjectReturnsItsExistingApplicationUser() {
        AppUser existingUser = new AppUser("google", "google-subject", "ayush@example.com", "Ayush");
        when(appUserRepository.findByAuthProviderAndAuthProviderSubject("google", "google-subject"))
                .thenReturn(Optional.of(existingUser));

        AppUser result = service.findOrCreateGoogleUser("google-subject", "ayush@example.com", "Ayush");

        assertThat(result).isSameAs(existingUser);
        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @Test
    void unknownProviderSubjectCreatesAnApplicationUser() {
        when(appUserRepository.findByAuthProviderAndAuthProviderSubject("google", "google-subject"))
                .thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = service.findOrCreateGoogleUser("google-subject", "ayush@example.com", "Ayush");

        assertThat(result.getAuthProvider()).isEqualTo("google");
        assertThat(result.getAuthProviderSubject()).isEqualTo("google-subject");
        assertThat(result.getEmail()).isEqualTo("ayush@example.com");
        verify(appUserRepository).save(any(AppUser.class));
    }
}
