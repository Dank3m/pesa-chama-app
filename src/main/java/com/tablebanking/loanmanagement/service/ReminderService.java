package com.tablebanking.loanmanagement.service;

import com.tablebanking.loanmanagement.entity.*;
import com.tablebanking.loanmanagement.entity.enums.*;
import com.tablebanking.loanmanagement.event.ContributionEvent;
import com.tablebanking.loanmanagement.event.LoanEvent;
import com.tablebanking.loanmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for sending reminder notifications via Kafka.
 * The notification microservice will consume these events and send SMS/Email.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderService {

    private final ContributionRepository contributionRepository;
    private final LoanRepository loanRepository;
    private final ContributionCycleRepository cycleRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.contribution-events:contribution-events}")
    private String contributionEventsTopic;

    @Value("${kafka.topics.loan-events:loan-events}")
    private String loanEventsTopic;

    @Value("${app.reminders.contribution-days-before:3}")
    private int contributionReminderDaysBefore;

    @Value("${app.reminders.loan-days-before:3}")
    private int loanReminderDaysBefore;

    /**
     * Send contribution reminders for contributions due soon.
     * Runs daily at 8 AM.
     */
    @Scheduled(cron = "${app.reminders.contribution-cron:0 0 8 * * ?}")
    @Transactional(readOnly = true)
    public void sendContributionReminders() {
        log.info("Starting contribution reminder job");

        LocalDate reminderDate = LocalDate.now().plusDays(contributionReminderDaysBefore);

        // Find active cycles with due date coming up
        List<ContributionCycle> upcomingCycles = cycleRepository.findByDueDateBetweenAndStatus(
                LocalDate.now(),
                reminderDate,
                CycleStatus.OPEN
        );

        int remindersSent = 0;
        for (ContributionCycle cycle : upcomingCycles) {
            // Get pending contributions for this cycle
            List<Contribution> pendingContributions = contributionRepository
                    .findByCycleAndStatusIn(cycle, List.of(ContributionStatus.PENDING, ContributionStatus.PARTIAL));

            for (Contribution contribution : pendingContributions) {
                try {
                    publishContributionReminder(contribution);
                    remindersSent++;
                } catch (Exception e) {
                    log.error("Failed to send reminder for contribution {}: {}",
                            contribution.getId(), e.getMessage());
                }
            }
        }

        log.info("Contribution reminder job completed. Sent {} reminders", remindersSent);
    }

    /**
     * Send loan payment reminders for loans with payments due soon.
     * Runs daily at 9 AM.
     */
    @Scheduled(cron = "${app.reminders.loan-cron:0 0 9 * * ?}")
    @Transactional(readOnly = true)
    public void sendLoanPaymentReminders() {
        log.info("Starting loan payment reminder job");

        // Find active loans
        List<Loan> activeLoans = loanRepository.findByStatus(LoanStatus.ACTIVE);

        int remindersSent = 0;
        for (Loan loan : activeLoans) {
            try {
                // Check if loan has outstanding balance
                if (loan.getOutstandingBalance().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    publishLoanPaymentReminder(loan);
                    remindersSent++;
                }
            } catch (Exception e) {
                log.error("Failed to send reminder for loan {}: {}", loan.getLoanNumber(), e.getMessage());
            }
        }

        log.info("Loan payment reminder job completed. Sent {} reminders", remindersSent);
    }

    /**
     * Send overdue contribution alerts.
     * Runs daily at 10 AM.
     */
    @Scheduled(cron = "${app.reminders.overdue-cron:0 0 10 * * ?}")
    @Transactional(readOnly = true)
    public void sendOverdueContributionAlerts() {
        log.info("Starting overdue contribution alert job");

        // Find cycles past due date
        List<ContributionCycle> overdueCycles = cycleRepository.findByDueDateBeforeAndStatus(
                LocalDate.now(),
                CycleStatus.OPEN
        );

        int alertsSent = 0;
        for (ContributionCycle cycle : overdueCycles) {
            List<Contribution> overdueContributions = contributionRepository
                    .findByCycleAndStatusIn(cycle, List.of(ContributionStatus.PENDING, ContributionStatus.PARTIAL));

            for (Contribution contribution : overdueContributions) {
                try {
                    publishContributionOverdueAlert(contribution);
                    alertsSent++;
                } catch (Exception e) {
                    log.error("Failed to send overdue alert for contribution {}: {}",
                            contribution.getId(), e.getMessage());
                }
            }
        }

        log.info("Overdue contribution alert job completed. Sent {} alerts", alertsSent);
    }

    /**
     * Send overdue loan payment alerts.
     * Runs daily at 11 AM.
     */
    @Scheduled(cron = "${app.reminders.loan-overdue-cron:0 0 11 * * ?}")
    @Transactional(readOnly = true)
    public void sendOverdueLoanAlerts() {
        log.info("Starting overdue loan alert job");

        List<Loan> overdueLoans = loanRepository.findByStatus(LoanStatus.DEFAULTED);

        int alertsSent = 0;
        for (Loan loan : overdueLoans) {
            try {
                publishLoanOverdueAlert(loan);
                alertsSent++;
            } catch (Exception e) {
                log.error("Failed to send overdue alert for loan {}: {}", loan.getLoanNumber(), e.getMessage());
            }
        }

        log.info("Overdue loan alert job completed. Sent {} alerts", alertsSent);
    }

    /**
     * Manual trigger for sending contribution reminders (for testing or admin use)
     */
    public int sendManualContributionReminders(UUID cycleId) {
        ContributionCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new RuntimeException("Cycle not found"));

        List<Contribution> pendingContributions = contributionRepository
                .findByCycleAndStatusIn(cycle, List.of(ContributionStatus.PENDING, ContributionStatus.PARTIAL));

        int sent = 0;
        for (Contribution contribution : pendingContributions) {
            try {
                publishContributionReminder(contribution);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send reminder: {}", e.getMessage());
            }
        }

        return sent;
    }

    /**
     * Manual trigger for sending loan reminders (for testing or admin use)
     */
    public int sendManualLoanReminders(UUID groupId) {
        List<Loan> activeLoans = loanRepository.findByMemberGroupIdAndStatus(groupId, LoanStatus.ACTIVE);

        int sent = 0;
        for (Loan loan : activeLoans) {
            try {
                publishLoanPaymentReminder(loan);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send reminder: {}", e.getMessage());
            }
        }

        return sent;
    }

    private void publishContributionReminder(Contribution contribution) {
        Member member = contribution.getMember();
        BankingGroup group = member.getGroup();
        ContributionCycle cycle = contribution.getCycle();

        ContributionEvent event = ContributionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("CONTRIBUTION_REMINDER")
                .contributionId(contribution.getId())
                .memberId(member.getId())
                .memberName(member.getFullName())
                .phoneNumber(member.getPhoneNumber())
                .email(member.getEmail())
                .groupId(group.getId())
                .groupName(group.getName())
                .cycleMonth(cycle.getCycleMonth())
                .dueDate(cycle.getDueDate())
                .expectedAmount(contribution.getExpectedAmount())
                .paidAmount(contribution.getPaidAmount())
                .status(contribution.getStatus().name())
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send(contributionEventsTopic, contribution.getId().toString(), event);
        log.debug("Published contribution reminder for member: {}", member.getFullName());
    }

    private void publishContributionOverdueAlert(Contribution contribution) {
        Member member = contribution.getMember();
        BankingGroup group = member.getGroup();
        ContributionCycle cycle = contribution.getCycle();

        ContributionEvent event = ContributionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("CONTRIBUTION_OVERDUE")
                .contributionId(contribution.getId())
                .memberId(member.getId())
                .memberName(member.getFullName())
                .phoneNumber(member.getPhoneNumber())
                .email(member.getEmail())
                .groupId(group.getId())
                .groupName(group.getName())
                .cycleMonth(cycle.getCycleMonth())
                .dueDate(cycle.getDueDate())
                .expectedAmount(contribution.getExpectedAmount())
                .paidAmount(contribution.getPaidAmount())
                .status(contribution.getStatus().name())
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send(contributionEventsTopic, contribution.getId().toString(), event);
        log.debug("Published contribution overdue alert for member: {}", member.getFullName());
    }

    private void publishLoanPaymentReminder(Loan loan) {
        Member member = loan.getMember();
        BankingGroup group = member.getGroup();

        LoanEvent event = LoanEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("LOAN_PAYMENT_REMINDER")
                .loanId(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .memberId(member.getId())
                .memberName(member.getFullName())
                .phoneNumber(member.getPhoneNumber())
                .email(member.getEmail())
                .groupId(group.getId())
                .groupName(group.getName())
                .amount(loan.getPrincipalAmount())
                .outstandingBalance(loan.getOutstandingBalance())
                .dueDate(loan.getExpectedEndDate())
                .disbursementDate(loan.getDisbursementDate())
                .status(loan.getStatus().name())
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send(loanEventsTopic, loan.getId().toString(), event);
        log.debug("Published loan payment reminder for member: {}", member.getFullName());
    }

    private void publishLoanOverdueAlert(Loan loan) {
        Member member = loan.getMember();
        BankingGroup group = member.getGroup();

        LoanEvent event = LoanEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("LOAN_OVERDUE")
                .loanId(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .memberId(member.getId())
                .memberName(member.getFullName())
                .phoneNumber(member.getPhoneNumber())
                .email(member.getEmail())
                .groupId(group.getId())
                .groupName(group.getName())
                .amount(loan.getPrincipalAmount())
                .outstandingBalance(loan.getOutstandingBalance())
                .dueDate(loan.getExpectedEndDate())
                .disbursementDate(loan.getDisbursementDate())
                .status(loan.getStatus().name())
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send(loanEventsTopic, loan.getId().toString(), event);
        log.debug("Published loan overdue alert for member: {}", member.getFullName());
    }
}