package com.finsight.finsight_ai.controller;

import com.finsight.finsight_ai.dto.request.BudgetRequest;
import com.finsight.finsight_ai.dto.response.BudgetResponse;
import com.finsight.finsight_ai.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            @Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(budgetService.createBudget(request));
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets(
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year) {

        int m = month == 0 ? LocalDate.now().getMonthValue() : month;
        int y = year == 0 ? LocalDate.now().getYear() : year;

        return ResponseEntity.ok(budgetService.getBudgets(m, y));
    }
}