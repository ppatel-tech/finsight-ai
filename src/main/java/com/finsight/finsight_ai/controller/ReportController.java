package com.finsight.finsight_ai.controller;

import com.finsight.finsight_ai.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/monthly")
    public ResponseEntity<byte[]> getMonthlyReport(
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year) {

        int m = month == 0 ? LocalDate.now().getMonthValue() : month;
        int y = year == 0 ? LocalDate.now().getYear() : year;

        byte[] pdf = reportService.generateMonthlyReport(m, y);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=report-" + m + "-" + y + ".pdf")
                .body(pdf);
    }
}