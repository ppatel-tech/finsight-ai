package com.finsight.finsight_ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyAnalyticsResponse {

    private int month;

    private int year;

    private BigDecimal totalSpent;

    private BigDecimal dailyAverage;

    private String highestSpendingCategory;

    private List<CategorySpendResponse> categoryBreakdown;


}
