package com.finsight.finsight_ai.dto.response;

import com.finsight.finsight_ai.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CategorySpendResponse {

    private Category category;

    private BigDecimal totalAmount;

    private long transactionCount;


}
