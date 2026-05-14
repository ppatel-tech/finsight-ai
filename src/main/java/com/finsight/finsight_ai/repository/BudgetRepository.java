package com.finsight.finsight_ai.repository;

import com.finsight.finsight_ai.entity.Budget;
import com.finsight.finsight_ai.entity.Category;
import com.finsight.finsight_ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserAndMonthAndYear(User user, int month, int year);

    Optional<Budget> findByUserAndCategoryAndMonthAndYear(
            User user, Category category, int month, int year);


}
