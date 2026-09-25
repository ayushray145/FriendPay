package com.splitledger.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_users", uniqueConstraints = @UniqueConstraint(
        name = "uk_app_users_provider_subject", columnNames = {"auth_provider", "auth_provider_subject"}))
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "auth_provider", nullable = false, length = 32)
    private String authProvider;

    @Column(name = "auth_provider_subject", nullable = false, length = 255)
    private String authProviderSubject;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppUser() {
    }

    public AppUser(String authProvider, String authProviderSubject, String email, String displayName) {
        this.authProvider = authProvider;
        this.authProviderSubject = authProviderSubject;
        this.email = email;
        this.displayName = displayName;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getAuthProvider() { return authProvider; }
    public String getAuthProviderSubject() { return authProviderSubject; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
