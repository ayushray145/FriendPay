package com.splitledger.friend;

import com.splitledger.user.AppUser;
import com.splitledger.user.AppUserRepository;
import com.splitledger.person.Person;
import com.splitledger.person.PersonRepository;
import com.splitledger.security.ApplicationUserNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendService {
    private static final List<FriendRequestStatus> OPEN_STATUSES =
            List.of(FriendRequestStatus.PENDING, FriendRequestStatus.ACCEPTED);

    private final FriendRequestRepository friendRequestRepository;
    private final AppUserRepository appUserRepository;
    private final PersonRepository personRepository;

    public FriendService(FriendRequestRepository friendRequestRepository, AppUserRepository appUserRepository,
                         PersonRepository personRepository) {
        this.friendRequestRepository = friendRequestRepository;
        this.appUserRepository = appUserRepository;
        this.personRepository = personRepository;
    }

    @Transactional
    public FriendRequestResponse send(UUID requesterId, CreateFriendRequest request) {
        AppUser requester = findUser(requesterId);
        AppUser recipient = appUserRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(FriendAccountNotFoundException::new);
        if (requester.getId().equals(recipient.getId())) {
            throw new FriendConflictException("You cannot send a friend request to yourself");
        }
        UUID lowId = min(requester.getId(), recipient.getId());
        UUID highId = max(requester.getId(), recipient.getId());
        if (friendRequestRepository.existsByPairLowUserIdAndPairHighUserIdAndStatusIn(lowId, highId, OPEN_STATUSES)) {
            throw new FriendConflictException("A friend request or friendship already exists for this account");
        }
        FriendRequest saved = friendRequestRepository.saveAndFlush(
                new FriendRequest(requester, recipient, request.nickname()));
        return requestView(saved, requesterId);
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> requests(UUID userId) {
        return friendRequestRepository.findAllByRequesterIdOrRecipientIdOrderByCreatedAtDesc(userId, userId).stream()
                .filter(request -> request.getStatus() == FriendRequestStatus.PENDING)
                .map(request -> requestView(request, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> friends(UUID userId) {
        return friendRequestRepository.findAllByRequesterIdAndStatusOrRecipientIdAndStatusOrderByCreatedAtDesc(
                        userId, FriendRequestStatus.ACCEPTED, userId, FriendRequestStatus.ACCEPTED).stream()
                .map(request -> friendView(request, userId))
                .sorted(Comparator.comparing(FriendResponse::nickname, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public FriendResponse accept(UUID userId, UUID requestId) {
        FriendRequest request = friendRequestRepository
                .findByIdAndRecipientIdAndStatus(requestId, userId, FriendRequestStatus.PENDING)
                .orElseThrow(() -> new FriendRequestNotFoundException(requestId));
        request.accept(request.getRequester().getDisplayName());
        addPrivateContact(userId, request.getRequester(), request.getRequester().getDisplayName());
        addPrivateContact(request.getRequester().getId(), request.getRecipient(),
                request.getRequesterNickname() == null ? request.getRecipient().getDisplayName() : request.getRequesterNickname());
        return friendView(friendRequestRepository.save(request), userId);
    }

    @Transactional
    public void decline(UUID userId, UUID requestId) {
        FriendRequest request = friendRequestRepository
                .findByIdAndRecipientIdAndStatus(requestId, userId, FriendRequestStatus.PENDING)
                .orElseThrow(() -> new FriendRequestNotFoundException(requestId));
        request.decline();
    }

    @Transactional
    public FriendResponse updateNickname(UUID userId, UUID requestId, UpdateFriendNicknameRequest update) {
        FriendRequest request = friendRequestRepository.findByIdAndStatus(requestId, FriendRequestStatus.ACCEPTED)
                .orElseThrow(() -> new FriendRequestNotFoundException(requestId));
        if (!request.getRequester().getId().equals(userId) && !request.getRecipient().getId().equals(userId)) {
            throw new FriendRequestNotFoundException(requestId);
        }
        request.updateNickname(userId, update.nickname());
        AppUser other = request.getRequester().getId().equals(userId) ? request.getRecipient() : request.getRequester();
        personRepository.findByOwnerIdAndLinkedUserId(userId, other.getId())
                .ifPresent(person -> person.rename(update.nickname().trim()));
        return friendView(friendRequestRepository.save(request), userId);
    }

    private AppUser findUser(UUID id) {
        return appUserRepository.findById(id).orElseThrow(() -> new ApplicationUserNotFoundException(id));
    }

    private FriendRequestResponse requestView(FriendRequest request, UUID viewerId) {
        boolean outgoing = request.getRequester().getId().equals(viewerId);
        AppUser other = outgoing ? request.getRecipient() : request.getRequester();
        String nickname = outgoing ? request.getRequesterNickname() : null;
        return new FriendRequestResponse(request.getId(), outgoing ? "OUTGOING" : "INCOMING",
                request.getStatus().name(), other.getEmail(), other.getDisplayName(), nickname, request.getCreatedAt());
    }

    private FriendResponse friendView(FriendRequest request, UUID viewerId) {
        boolean requester = request.getRequester().getId().equals(viewerId);
        AppUser other = requester ? request.getRecipient() : request.getRequester();
        String nickname = requester ? request.getRequesterNickname() : request.getRecipientNickname();
        return new FriendResponse(request.getId(), other.getId(), other.getEmail(), other.getDisplayName(),
                nickname == null || nickname.isBlank() ? other.getDisplayName() : nickname, request.getCreatedAt());
    }

    private UUID min(UUID left, UUID right) { return left.compareTo(right) < 0 ? left : right; }
    private UUID max(UUID left, UUID right) { return left.compareTo(right) > 0 ? left : right; }

    private void addPrivateContact(UUID ownerId, AppUser otherUser, String nickname) {
        AppUser owner = findUser(ownerId);
        Person person = personRepository.findByOwnerIdAndLinkedUserId(ownerId, otherUser.getId())
                .orElseGet(() -> new Person(owner, otherUser, nickname));
        person.rename(nickname);
        personRepository.save(person);
    }
}
