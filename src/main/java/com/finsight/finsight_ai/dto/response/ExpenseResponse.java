package com.finsight.finsight_ai.dto.response;

import com.finsight.finsight_ai.entity.Category;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ExpenseResponse {
    private Long id;
    private BigDecimal amount;
    private String description;
    private Category category;
    private LocalDate expenseDate;
    private LocalDateTime createdAt;
}



