package com.finsight.finsight_ai.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class MonthlyAnalyticsResponse {

    private int month;

    private int year;

    private BigDecimal totalSpent;

    private BigDecimal dailyAverage;

    private String highestSpendingCategory;

    private List<CategorySpendResponse> categoryBreakdown;


}
