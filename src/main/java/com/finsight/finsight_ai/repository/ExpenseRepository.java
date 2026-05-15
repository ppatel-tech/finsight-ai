package com.finsight.finsight_ai.repository;

import com.finsight.finsight_ai.dto.response.CategorySpendResponse;
import com.finsight.finsight_ai.entity.Category;
import com.finsight.finsight_ai.entity.Expense;
import com.finsight.finsight_ai.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Page<Expense> findByUserOrderByExpenseDateDesc(User user, Pageable pageable);

    Page<Expense> findByUserAndCategoryOrderByExpenseDateDesc(
            User user, Category category, Pageable pageable);

    List<Expense> findByUserAndExpenseDateBetween(
            User user, LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user = :user " +
            "AND e.category = :category " +
            "AND MONTH(e.expenseDate) = :month AND YEAR(e.expenseDate) = :year")
    BigDecimal sumByUserAndCategoryAndMonth(
            User user, Category category, int month, int year);


    // Total spent in a month
    @Query("SELECT SUM(e.amount) FROM Expense e " +
            "WHERE e.user = :user " +
            "AND MONTH(e.expenseDate) = :month " +
            "AND YEAR(e.expenseDate) = :year")
    BigDecimal sumByUserAndMonth(User user, int month, int year);



    // Spending grouped by category for a month
    @Query("SELECT new com.finsight.finsight_ai.dto.response.CategorySpendResponse(" +
            "e.category, SUM(e.amount), COUNT(e)) " +
            "FROM Expense e " +
            "WHERE e.user = :user " +
            "AND MONTH(e.expenseDate) = :month " +
            "AND YEAR(e.expenseDate) = :year " +
            "GROUP BY e.category " +
            "ORDER BY SUM(e.amount) DESC")
    List<CategorySpendResponse> findCategoryBreakdown(
            User user, int month, int year);


    // Count of expense days in a month (for daily average calculation)
    @Query("SELECT COUNT(DISTINCT e.expenseDate) FROM Expense e " +
            "WHERE e.user = :user " +
            "AND MONTH(e.expenseDate) = :month " +
            "AND YEAR(e.expenseDate) = :year")
    long countDistinctExpenseDays(User user, int month, int year);



}