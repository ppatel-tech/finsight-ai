package com.finsight.finsight_ai.controller;
import com.finsight.finsight_ai.dto.response.MonthlyAnalyticsResponse;
import com.finsight.finsight_ai.entity.User;
import com.finsight.finsight_ai.exception.ResourceNotFoundException;
import com.finsight.finsight_ai.repository.UserRepository;
import com.finsight.finsight_ai.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final UserRepository userRepository;
    private final AnalyticsService analyticsService;

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyAnalyticsResponse> getMonthlyAnalytics(
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year) {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        int m = month == 0 ? LocalDate.now().getMonthValue() : month;
        int y = year == 0 ? LocalDate.now().getYear() : year;

        return ResponseEntity.ok(analyticsService.getMonthlyAnalytics(user, m, y));
    }
}