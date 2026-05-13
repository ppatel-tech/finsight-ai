package com.finsight.finsight_ai.controller;

import com.finsight.finsight_ai.dto.request.ExpenseRequest;
import com.finsight.finsight_ai.dto.response.ExpenseResponse;
import com.finsight.finsight_ai.entity.Category;
import com.finsight.finsight_ai.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseResponse> addExpense(@Valid @RequestBody ExpenseRequest request){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.addExpense(request));

    }

    @GetMapping
    public ResponseEntity<Page<ExpenseResponse>> getExpense(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Category category){
        return ResponseEntity.ok(expenseService.getExpenses(page, size,category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request){
        return ResponseEntity.ok(expenseService.updateExpense(id,request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExponse(@PathVariable Long id){
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

}



















