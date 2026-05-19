package com.finsight.finsight_ai.scheduler;

import com.finsight.finsight_ai.entity.Budget;
import com.finsight.finsight_ai.repository.BudgetRepository;
import com.finsight.finsight_ai.service.BudgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetAlertScheduler {

    private final BudgetRepository budgetRepository;
    private final BudgetService budgetService;

    // Runs every day at midnight
    @Scheduled(cron = "0 0 0 * * *")
    public void runDailyBudgetCheck() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();

        log.info("Daily budget check started for {}/{}", month, year);

        List<Budget> allBudgets = budgetRepository
                .findAll()
                .stream()
                .filter(b -> b.getMonth() == month && b.getYear() == year)
                .toList();

        for (Budget budget : allBudgets) {
            budgetService.checkBudgetAlert(
                    budget.getUser(),
                    budget.getCategory(),
                    month,
                    year
            );
        }

        log.info("Daily budget check completed. Checked {} budgets.",
                allBudgets.size());
    }

    // Runs every minute — for testing only, remove in production
    @Scheduled(fixedRate = 60000)
    public void runMinuteBudgetCheckForTesting() {
        log.info("Scheduled check running — {} active budgets monitored",
                budgetRepository.count());
    }
}