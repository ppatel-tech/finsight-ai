package com.finsight.finsight_ai.dto.request;

import com.finsight.finsight_ai.entity.Category;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater then 0")
    @Digits(integer = 8, fraction = 2 , message = "Invalid amount format")
    private BigDecimal amount;

    @Size(max = 255, message = "Description too long")
    private String description;

    @NotNull(message = "Category is required")
    private Category category;

    @NotNull(message = "date is required")
    @PastOrPresent(message = "expense date cannot be in the future")
    private LocalDate expenseDate;



}
