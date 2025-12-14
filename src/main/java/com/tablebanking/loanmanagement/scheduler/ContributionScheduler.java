package com.tablebanking.loanmanagement.scheduler;

import com.tablebanking.loanmanagement.entity.ContributionCycle;
import com.tablebanking.loanmanagement.entity.enums.CycleStatus;
import com.tablebanking.loanmanagement.repository.ContributionCycleRepository;
import com.tablebanking.loanmanagement.service.ContributionService;
import com.tablebanking.loanmanagement.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduler for contribution cycle processing.
 * Handles end-of-month processing to convert unpaid contributions to loans.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContributionScheduler {

    private final ContributionCycleRepository cycleRepository;
    private final ContributionService contributionService;
    private final ReminderService reminderService;

    /**
     * Process overdue contribution cycles.
     * Runs at midnight on the 1st of each month to process the previous month's cycle.
     * Also runs daily at midnight to catch any missed cycles.
     */
    @Scheduled(cron = "${app.scheduler.contribution-check-cron:0 0 0 1 * ?}")
    public void processOverdueCycles() {
        log.info("Starting overdue contribution cycles processing");

        LocalDate today = LocalDate.now();
        List<ContributionCycle> overdueCycles = cycleRepository.findOverdueCycles(today);

        int processedCount = 0;
        int errorCount = 0;

        for (ContributionCycle cycle : overdueCycles) {
            try {
                log.info("Processing overdue cycle: {} (Due: {})",
                        cycle.getCycleMonth(), cycle.getDueDate());

                int defaultedCount = contributionService.processDefaultedContributions(cycle.getId());

                log.info("Processed cycle {}: {} contributions converted to loans",
                        cycle.getCycleMonth(), defaultedCount);
                processedCount++;
            } catch (Exception e) {
                errorCount++;
                log.error("Failed to process cycle {}: {}",
                        cycle.getCycleMonth(), e.getMessage());
            }
        }

        log.info("Overdue cycles processing completed: {} processed, {} errors",
                processedCount, errorCount);
    }

    /**
     * Daily check for any cycles that need processing.
     * Runs at midnight every day as a safety net.
     */
    @Scheduled(cron = "0 30 0 * * ?") // 12:30 AM daily
    public void dailyCycleCheck() {
        log.debug("Running daily contribution cycle check");

        LocalDate today = LocalDate.now();
        List<ContributionCycle> unprocessedCycles = cycleRepository.findOverdueCycles(today);

        if (!unprocessedCycles.isEmpty()) {
            log.warn("Found {} unprocessed overdue cycles", unprocessedCycles.size());
            processOverdueCycles();
        }
    }

    /**
     * Send contribution reminders.
     * Runs at 9 AM on the 25th of each month.
     */
    @Scheduled(cron = "0 0 9 25 * ?")
    public void sendContributionReminders() {
        log.info("Sending contribution reminders for 25th of month");

        List<ContributionCycle> openCycles = cycleRepository.findByStatus(CycleStatus.OPEN);

        int totalRemindersSent = 0;

        for (ContributionCycle cycle : openCycles) {
            if (cycle.getDueDate().isAfter(LocalDate.now())) {
                var pendingContributions = contributionService.getPendingContributions(cycle.getId());

                log.info("Cycle {}: {} pending contributions",
                        cycle.getCycleMonth(), pendingContributions.size());

                if (!pendingContributions.isEmpty()) {
                    try {
                        int remindersSent = reminderService.sendManualContributionReminders(cycle.getId());
                        totalRemindersSent += remindersSent;
                        log.info("Sent {} reminders for cycle {}", remindersSent, cycle.getCycleMonth());
                    } catch (Exception e) {
                        log.error("Failed to send reminders for cycle {}: {}",
                                cycle.getCycleMonth(), e.getMessage(), e);
                    }
                }
            }
        }

        log.info("Contribution reminders processing completed. Total reminders sent: {}", totalRemindersSent);
    }
}