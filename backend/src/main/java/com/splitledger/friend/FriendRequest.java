package com.splitledger.friend;

import com.splitledger.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "friend_requests")
public class FriendRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private AppUser requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private AppUser recipient;

    @Column(name = "pair_low_user_id", nullable = false)
    private UUID pairLowUserId;

    @Column(name = "pair_high_user_id", nullable = false)
    private UUID pairHighUserId;

    @Column(name = "requester_nickname", length = 80)
    private String requesterNickname;

    @Column(name = "recipient_nickname", length = 80)
    private String recipientNickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FriendRequestStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FriendRequest() { }

    public FriendRequest(AppUser requester, AppUser recipient, String requesterNickname) {
        this.requester = requester;
        this.recipient = recipient;
        if (requester.getId().compareTo(recipient.getId()) < 0) {
            this.pairLowUserId = requester.getId();
            this.pairHighUserId = recipient.getId();
        } else {
            this.pairLowUserId = recipient.getId();
            this.pairHighUserId = requester.getId();
        }
        this.requesterNickname = normalize(requesterNickname);
        this.status = FriendRequestStatus.PENDING;
    }

    public void accept(String recipientDisplayName) {
        status = FriendRequestStatus.ACCEPTED;
        if (recipientNickname == null) recipientNickname = recipientDisplayName;
    }

    public void decline() { status = FriendRequestStatus.DECLINED; }

    public void updateNickname(UUID ownerId, String nickname) {
        if (requester.getId().equals(ownerId)) requesterNickname = normalize(nickname);
        else if (recipient.getId().equals(ownerId)) recipientNickname = normalize(nickname);
    }

    private String normalize(String nickname) {
        if (nickname == null || nickname.isBlank()) return null;
        return nickname.trim();
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public AppUser getRequester() { return requester; }
    public AppUser getRecipient() { return recipient; }
    public String getRequesterNickname() { return requesterNickname; }
    public String getRecipientNickname() { return recipientNickname; }
    public FriendRequestStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
