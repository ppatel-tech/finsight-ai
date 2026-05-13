package com.finsight.finsight_ai.service;

import com.finsight.finsight_ai.dto.request.ExpenseRequest;
import com.finsight.finsight_ai.dto.response.ExpenseResponse;
import com.finsight.finsight_ai.entity.Category;
import com.finsight.finsight_ai.entity.Expense;
import com.finsight.finsight_ai.entity.User;
import com.finsight.finsight_ai.repository.ExpenseRepository;
import com.finsight.finsight_ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    // Helper — gets the currently logged-in user from SecurityContext
    //-------------- get the user by Email ---------------------------

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }



    // ------------ Add expense -------------------------------
    @Transactional
    public ExpenseResponse addExpense(ExpenseRequest request) {
        User user = getCurrentUser();

        Expense expense = Expense.builder()
                .amount(request.getAmount())
                .description(request.getDescription())
                .category(request.getCategory())
                .expenseDate(request.getExpenseDate())
                .user(user)
                .build();

        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }




// --------------- Get Expenses ----------------------------

    public Page<ExpenseResponse> getExpenses(int page, int size, Category category) {
        User user = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("expenseDate").descending());

        Page<Expense> expenses;
        if (category != null) {
            expenses = expenseRepository
                    .findByUserAndCategoryOrderByExpenseDateDesc(user, category, pageable);
        } else {
            expenses = expenseRepository
                    .findByUserOrderByExpenseDateDesc(user, pageable);
        }

        return expenses.map(this::toResponse);
    }





    // -------------------- Update Expenses -----------------------------

    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {
        User user = getCurrentUser();

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        // SECURITY CHECK — user can only edit their own expenses
        if (!expense.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        expense.setAmount(request.getAmount());
        expense.setDescription(request.getDescription());
        expense.setCategory(request.getCategory());
        expense.setExpenseDate(request.getExpenseDate());

        return toResponse(expenseRepository.save(expense));
    }



    //---------------- Delete Expense --------------------------

    @Transactional
    public void deleteExpense(Long id) {
        User user = getCurrentUser();

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (!expense.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        expenseRepository.delete(expense);
    }

    //------- Maps entity  to DTO , never expose the entity directly --------
    private ExpenseResponse toResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .description(expense.getDescription())
                .category(expense.getCategory())
                .expenseDate(expense.getExpenseDate())
                .createdAt(expense.getCreatedAt())
                .build();
    }
}