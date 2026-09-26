package com.splitledger.friend;

import com.splitledger.ledger.DebtDirection;
import com.splitledger.security.ApplicationUserNotFoundException;
import com.splitledger.user.AppUser;
import com.splitledger.user.AppUserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendSettlementReportService {
    private final FriendRequestRepository friendRequestRepository;
    private final FriendSettlementReportRepository reportRepository;
    private final FriendSettlementRepository settlementRepository;
    private final FriendLedgerService ledgerService;
    private final AppUserRepository userRepository;

    public FriendSettlementReportService(FriendRequestRepository friendRequestRepository,
                                         FriendSettlementReportRepository reportRepository,
                                         FriendSettlementRepository settlementRepository,
                                         FriendLedgerService ledgerService, AppUserRepository userRepository) {
        this.friendRequestRepository = friendRequestRepository;
        this.reportRepository = reportRepository;
        this.settlementRepository = settlementRepository;
        this.ledgerService = ledgerService;
        this.userRepository = userRepository;
    }

    @Transactional
    public FriendSettlementReportResponse create(UUID payerId, CreateFriendSettlementReportRequest request) {
        FriendRequest friendship = friendRequestRepository.findBetweenUsersWithStatusForUpdate(
                        payerId, request.friendUserId(), FriendRequestStatus.ACCEPTED)
                .orElseThrow(() -> new FriendConflictException("Payment reports require an accepted friendship"));
        if (reportRepository.existsByPayerIdAndRecipientIdAndStatus(
                payerId, request.friendUserId(), FriendSettlementReportStatus.PENDING)) {
            throw new FriendConflictException("A payment report is already awaiting approval from this friend");
        }
        BigDecimal outstanding = ledgerService.sharedOutstanding(
                payerId, request.friendUserId(), DebtDirection.USER_OWES_PERSON);
        BigDecimal reverseOutstanding = ledgerService.sharedOutstanding(
                request.friendUserId(), payerId, DebtDirection.USER_OWES_PERSON);
        BigDecimal netPayable = outstanding.subtract(reverseOutstanding).max(BigDecimal.ZERO);
        if (request.amount().compareTo(netPayable) > 0) {
            throw new FriendConflictException("Reported amount exceeds the outstanding balance");
        }
        AppUser payer = userRepository.findById(payerId)
                .orElseThrow(() -> new ApplicationUserNotFoundException(payerId));
        AppUser recipient = userRepository.findById(request.friendUserId())
                .orElseThrow(() -> new FriendConflictException("Friend account is unavailable"));
        FriendSettlementReport report = reportRepository.save(
                new FriendSettlementReport(friendship, payer, recipient, request.amount(), request.paymentReference()));
        return FriendSettlementReportResponse.from(report, payerId);
    }

    @Transactional(readOnly = true)
    public List<FriendSettlementReportResponse> list(UUID viewerId) {
        return reportRepository.findAllByPayerIdOrRecipientIdOrderByCreatedAtDesc(viewerId, viewerId).stream()
                .map(report -> FriendSettlementReportResponse.from(report, viewerId)).toList();
    }

    @Transactional
    public FriendSettlementReportResponse approve(UUID recipientId, UUID reportId) {
        FriendSettlementReport report = findForUpdate(reportId);
        if (!report.getRecipient().getId().equals(recipientId)) throw new FriendSettlementReportNotFoundException(reportId);
        if (report.getStatus() != FriendSettlementReportStatus.PENDING) {
            throw new FriendConflictException("Only pending payment reports can be approved");
        }
        UUID payerId = report.getPayer().getId();
        UUID actualRecipientId = report.getRecipient().getId();
        FriendRequest friendship = friendRequestRepository.findBetweenUsersWithStatusForUpdate(
                        payerId, actualRecipientId, FriendRequestStatus.ACCEPTED)
                .orElseThrow(() -> new FriendConflictException("This friend connection is no longer active"));
        BigDecimal outstanding = ledgerService.sharedOutstanding(
                payerId, actualRecipientId, DebtDirection.USER_OWES_PERSON);
        if (report.getAmount().compareTo(outstanding) > 0) {
            throw new FriendConflictException("Reported amount now exceeds the outstanding balance");
        }
        settlementRepository.save(new FriendSettlement(friendship, report.getPayer(), report.getRecipient(),
                report.getAmount(), Instant.now(), report.getRecipient()));
        report.approve();
        return FriendSettlementReportResponse.from(reportRepository.save(report), recipientId);
    }

    @Transactional
    public FriendSettlementReportResponse reject(UUID recipientId, UUID reportId) {
        FriendSettlementReport report = findForUpdate(reportId);
        if (!report.getRecipient().getId().equals(recipientId)) throw new FriendSettlementReportNotFoundException(reportId);
        if (report.getStatus() != FriendSettlementReportStatus.PENDING) {
            throw new FriendConflictException("Only pending payment reports can be rejected");
        }
        report.reject();
        return FriendSettlementReportResponse.from(reportRepository.save(report), recipientId);
    }

    private FriendSettlementReport findForUpdate(UUID reportId) {
        return reportRepository.findByIdForUpdate(reportId)
                .orElseThrow(() -> new FriendSettlementReportNotFoundException(reportId));
    }
}
