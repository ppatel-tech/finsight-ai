package com.finsight.finsight_ai.service;

import com.finsight.finsight_ai.dto.response.MonthlyAnalyticsResponse;
import com.finsight.finsight_ai.entity.User;
import com.finsight.finsight_ai.exception.ResourceNotFoundException;
import com.finsight.finsight_ai.repository.UserRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Month;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public byte[] generateMonthlyReport(int month, int year) {
        User user = getCurrentUser();
        MonthlyAnalyticsResponse analytics =
                analyticsService.getMonthlyAnalytics(user, month, year);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Title
            document.add(new Paragraph("FinSight AI — Monthly Report")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(
                    Month.of(month).name() + " " + year + " | " + user.getName())
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY));

            document.add(new Paragraph("\n"));

            // Summary section
            document.add(new Paragraph("Summary")
                    .setFontSize(14).setBold());

            document.add(new Paragraph(
                    "Total Spent: ₹" + analytics.getTotalSpent()));
            document.add(new Paragraph(
                    "Daily Average: ₹" + analytics.getDailyAverage()));
            document.add(new Paragraph(
                    "Highest Spending Category: " +
                            analytics.getHighestSpendingCategory()));

            document.add(new Paragraph("\n"));

            // Category breakdown table
            document.add(new Paragraph("Category Breakdown")
                    .setFontSize(14).setBold());

            Table table = new Table(
                    UnitValue.createPercentArray(new float[]{40, 30, 30}))
                    .setWidth(UnitValue.createPercentValue(100));

            // Table headers
            table.addHeaderCell(new Cell().add(
                            new Paragraph("Category").setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(
                            new Paragraph("Amount").setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(
                            new Paragraph("Transactions").setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY));

            // Table rows
            analytics.getCategoryBreakdown().forEach(cat -> {
                table.addCell(cat.getCategory().name());
                table.addCell("₹" + cat.getTotalAmount());
                table.addCell(String.valueOf(cat.getTransactionCount()));
            });

            document.add(table);
            document.close();

            log.info("PDF report generated for user: {}, {}/{}",
                    user.getEmail(), month, year);

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate PDF report", e);
            throw new RuntimeException("Failed to generate report");
        }
    }
}