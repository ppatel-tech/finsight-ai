package com.finsight.finsight_ai.dto.request;

import com.finsight.finsight_ai.entity.Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
@Data
public class BudgetRequest {

    @NotNull(message = "categoy is required")
    private Category category;

    @NotNull(message = "limit amount is required")
    @DecimalMin(value = "1.00", message = "Bedget must be at least 1.00")
    private BigDecimal limitAmount;

    @NotNull(message = "month is required")
    @Min(value = 1) @Max(value = 12)
    private Integer month;

    @NotNull(message = "year is required")
    @Min(value = 2020)
    private Integer year;


}
