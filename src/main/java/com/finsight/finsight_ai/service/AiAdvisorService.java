package com.finsight.finsight_ai.service;

import com.finsight.finsight_ai.ai.FinancialAdvisorAi;
import com.finsight.finsight_ai.dto.request.AiAdviceRequest;
import com.finsight.finsight_ai.dto.response.AiAdviceResponse;
import com.finsight.finsight_ai.dto.response.BudgetResponse;
import com.finsight.finsight_ai.dto.response.CategorySpendResponse;
import com.finsight.finsight_ai.dto.response.MonthlyAnalyticsResponse;
import com.finsight.finsight_ai.entity.User;
import com.finsight.finsight_ai.exception.ResourceNotFoundException;
import com.finsight.finsight_ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAdvisorService {

    private final FinancialAdvisorAi financialAdvisorAi;
    private final AnalyticsService analyticsService;
    private final BudgetService budgetService;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public AiAdviceResponse getAdvice(AiAdviceRequest request) {
        User user = getCurrentUser();
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        // Step 1: Fetch real financial data
        MonthlyAnalyticsResponse analytics =
                analyticsService.getMonthlyAnalytics(user, month, year);
        List<BudgetResponse> budgets =
                budgetService.getBudgets(month, year);

        // Step 2: Format context strings
        String categoryBreakdown = formatCategoryBreakdown(
                analytics.getCategoryBreakdown());
        String budgetStatus = formatBudgetStatus(budgets);

        log.info("AI advice requested by user: {} — question: {}",
                user.getEmail(), request.getQuestion());

        // Step 3: Call Gemini with real data as context
        String advice = financialAdvisorAi.advise(
                String.valueOf(month),
                String.valueOf(year),
                analytics.getTotalSpent().toString(),
                analytics.getDailyAverage().toString(),
                analytics.getHighestSpendingCategory(),
                categoryBreakdown,
                budgetStatus,
                request.getQuestion()
        );

        return AiAdviceResponse.builder()
                .question(request.getQuestion())
                .advice(advice)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String formatCategoryBreakdown(
            List<CategorySpendResponse> breakdown) {
        if (breakdown.isEmpty()) return "No expenses recorded this month.";

        StringBuilder sb = new StringBuilder();
        breakdown.forEach(cat ->
                sb.append("  - ")
                        .append(cat.getCategory().name())
                        .append(": ₹")
                        .append(cat.getTotalAmount())
                        .append(" (")
                        .append(cat.getTransactionCount())
                        .append(" transactions)\n")
        );
        return sb.toString();
    }

    private String formatBudgetStatus(List<BudgetResponse> budgets) {
        if (budgets.isEmpty()) return "No budgets set for this month.";

        StringBuilder sb = new StringBuilder();
        budgets.forEach(b ->
                sb.append("  - ")
                        .append(b.getCategory().name())
                        .append(": ₹")
                        .append(b.getSpentAmount())
                        .append(" of ₹")
                        .append(b.getLimitAmount())
                        .append(" (")
                        .append(String.format("%.1f", b.getUsagePercentage()))
                        .append("%) — ")
                        .append(b.getAlertStatus())
                        .append("\n")
        );
        return sb.toString();
    }
}