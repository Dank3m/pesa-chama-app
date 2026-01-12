package com.tablebanking.loanmanagement.scheduler;

import com.tablebanking.loanmanagement.entity.Loan;
import com.tablebanking.loanmanagement.service.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduler for daily loan interest accrual.
 * Runs daily to calculate and apply compound interest to all active loans.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoanScheduler {

    private final LoanService loanService;

    /**
     * Daily interest accrual job.
     * Runs at 1 AM every day to accrue interest on all active loans.
     */
    @Scheduled(cron = "${app.scheduler.interest-accrual-cron:0 30 1 * * ?}")
    @Transactional
    public void accrueInterestDaily() {
        log.info("Starting daily interest accrual job");
        
        LocalDate accrualDate = LocalDate.now().minusDays(1); // Accrue for yesterday
        List<Loan> activeLoans = loanService.getLoansForInterestAccrual(accrualDate);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (Loan loan : activeLoans) {
            try {
                loanService.accrueInterestForLoan(loan, accrualDate);
                successCount.incrementAndGet();
            } catch (Exception e) {
                errorCount.incrementAndGet();
                log.error("Failed to accrue interest for loan {}: {}", 
                        loan.getLoanNumber(), e.getMessage());
            }
        }

        log.info("Daily interest accrual completed: {} successful, {} errors out of {} loans",
                successCount.get(), errorCount.get(), activeLoans.size());
    }

    /**
     * Check for overdue loans.
     * Runs at 2 AM daily.
     */
    @Scheduled(cron = "${app.scheduler.overdue-check-cron:0 0 2 * * ?}")
    @Transactional(readOnly = true)
    public void checkOverdueLoans() {
        log.info("Starting overdue loans check");
        
        var overdueLoans = loanService.getOverdueLoans();
        
        if (!overdueLoans.isEmpty()) {
            log.warn("Found {} overdue loans", overdueLoans.size());
            overdueLoans.forEach(loan -> 
                log.warn("Overdue loan: {} - Member: {} - Outstanding: {}", 
                        loan.getLoanNumber(), loan.getMemberName(), loan.getOutstandingBalance())
            );
        }

        log.info("Overdue loans check completed");
    }
}
