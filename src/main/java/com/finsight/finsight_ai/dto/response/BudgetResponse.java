package com.finsight.finsight_ai.dto.response;

import com.finsight.finsight_ai.entity.Category;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BudgetResponse {

    private Long id;
    private Category category;
    private BigDecimal limitAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private double usagePercentage;
    private int month;
    private int year;
    private String alertStatus; //"safe", "warning", "exceeded"
}


