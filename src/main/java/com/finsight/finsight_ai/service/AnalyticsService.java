package com.finsight.finsight_ai.service;

import com.finsight.finsight_ai.dto.response.CategorySpendResponse;
import com.finsight.finsight_ai.dto.response.MonthlyAnalyticsResponse;
import com.finsight.finsight_ai.entity.User;
import com.finsight.finsight_ai.exception.ResourceNotFoundException;
import com.finsight.finsight_ai.repository.ExpenseRepository;
import com.finsight.finsight_ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public MonthlyAnalyticsResponse getMonthlyAnalytics(int month, int year) {
        User user = getCurrentUser();

        // Total spent this month
        BigDecimal totalSpent = expenseRepository
                .sumByUserAndMonth(user, month, year);
        totalSpent = totalSpent != null ? totalSpent : BigDecimal.ZERO;

        // Category breakdown — ordered by highest spend
        List<CategorySpendResponse> breakdown = expenseRepository
                .findCategoryBreakdown(user, month, year);

        // Daily average — based on days that had actual expenses
        long activeDays = expenseRepository
                .countDistinctExpenseDays(user, month, year);

        BigDecimal dailyAverage = BigDecimal.ZERO;
        if (activeDays > 0) {
            dailyAverage = totalSpent
                    .divide(BigDecimal.valueOf(activeDays), 2, RoundingMode.HALF_UP);
        }

        // Highest spending category — first in list since ordered DESC
        String highestCategory = breakdown.isEmpty()
                ? "None"
                : breakdown.get(0).getCategory().name();

        return MonthlyAnalyticsResponse.builder()
                .month(month)
                .year(year)
                .totalSpent(totalSpent)
                .dailyAverage(dailyAverage)
                .highestSpendingCategory(highestCategory)
                .categoryBreakdown(breakdown)
                .build();
    }
}