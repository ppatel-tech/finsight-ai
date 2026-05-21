package com.finsight.finsight_ai.service;

import com.finsight.finsight_ai.dto.request.BudgetRequest;
import com.finsight.finsight_ai.dto.response.BudgetResponse;
import com.finsight.finsight_ai.entity.Budget;
import com.finsight.finsight_ai.entity.Category;
import com.finsight.finsight_ai.entity.User;
import com.finsight.finsight_ai.exception.DuplicateResourceException;
import com.finsight.finsight_ai.exception.ResourceNotFoundException;
import com.finsight.finsight_ai.repository.BudgetRepository;
import com.finsight.finsight_ai.repository.ExpenseRepository;
import com.finsight.finsight_ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    private static final double WARNING_THRESHOLD = 80.0;
    private static final double EXCEEDED_THRESHOLD = 100.0;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public BudgetResponse createBudget(BudgetRequest request) {
        User user = getCurrentUser();

        // Enforce uniqueness at service level before DB constraint fires
        budgetRepository.findByUserAndCategoryAndMonthAndYear(
                        user, request.getCategory(), request.getMonth(), request.getYear())
                .ifPresent(b -> {
                    throw new DuplicateResourceException(
                            "Budget for " + request.getCategory() +
                                    " in " + request.getMonth() + "/" + request.getYear() +
                                    " already exists");
                });

        Budget budget = Budget.builder()
                .user(user)
                .category(request.getCategory())
                .limitAmount(request.getLimitAmount())
                .month(request.getMonth())
                .year(request.getYear())
                .build();

        Budget saved = budgetRepository.save(budget);
        return buildResponse(saved, user);
    }

    public List<BudgetResponse> getBudgets(int month, int year) {
        User user = getCurrentUser();
        return budgetRepository.findByUserAndMonthAndYear(user, month, year)
                .stream()
                .map(b -> buildResponse(b, user))
                .toList();
    }

    // Called automatically after every expense is added
    public void checkBudgetAlert(User user, Category category,
                                 int month, int year) {
        budgetRepository.findByUserAndCategoryAndMonthAndYear(
                        user, category, month, year)
                .ifPresent(budget -> {
                    BigDecimal spent = getSpentAmount(user, category, month, year);
                    double percentage = spent
                            .divide(budget.getLimitAmount(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();

                    if (percentage >= EXCEEDED_THRESHOLD) {
                        log.warn("BUDGET EXCEEDED — User: {}, Category: {}, " +
                                        "Spent: {}, Limit: {}, Usage: {}%",
                                user.getEmail(), category, spent,
                                budget.getLimitAmount(),
                                String.format("%.1f", percentage));
                    } else if (percentage >= WARNING_THRESHOLD) {
                        log.warn("BUDGET WARNING — User: {}, Category: {}, " +
                                        "Spent: {}, Limit: {}, Usage: {}%",
                                user.getEmail(), category, spent,
                                budget.getLimitAmount(),
                                String.format("%.1f", percentage));
                    }
                });
    }

    private BigDecimal getSpentAmount(User user, Category category,
                                      int month, int year) {
        BigDecimal spent = expenseRepository
                .sumByUserAndCategoryAndMonth(user, category, month, year);
        return spent != null ? spent : BigDecimal.ZERO;
    }

    private BudgetResponse buildResponse(Budget budget, User user) {
        BigDecimal spent = getSpentAmount(
                user, budget.getCategory(),
                budget.getMonth(), budget.getYear());

        BigDecimal limit = budget.getLimitAmount();
        BigDecimal remaining = limit.subtract(spent).max(BigDecimal.ZERO);

        double percentage = spent
                .divide(limit, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

        String alertStatus;
        if (percentage >= EXCEEDED_THRESHOLD) alertStatus = "EXCEEDED";
        else if (percentage >= WARNING_THRESHOLD) alertStatus = "WARNING";
        else alertStatus = "SAFE";

        return BudgetResponse.builder()
                .id(budget.getId())
                .category(budget.getCategory())
                .limitAmount(limit)
                .spentAmount(spent)
                .remainingAmount(remaining)
                .usagePercentage(Math.min(percentage, 100.0))
                .month(budget.getMonth())
                .year(budget.getYear())
                .alertStatus(alertStatus)
                .build();
    }
}