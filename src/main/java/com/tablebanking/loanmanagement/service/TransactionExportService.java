package com.tablebanking.loanmanagement.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.TransactionResponse;
import com.tablebanking.loanmanagement.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionExportService {

    private final TransactionService transactionService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String[] HEADERS = {
            "Transaction #", "Date", "Member", "Type", "Category", "Description", "Amount"
    };

    /**
     * Export transactions to CSV
     */
    public byte[] exportToCsv(UUID groupId, UUID memberId, String type, String search) {
        List<TransactionResponse> transactions = transactionService.getTransactionsForExport(groupId, memberId, type, search);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(out)) {

            // Write header
            writer.println(String.join(",", HEADERS));

            // Write data rows
            for (TransactionResponse t : transactions) {
                writer.println(String.join(",",
                        escapeCSV(t.getTransactionNumber()),
                        formatDate(t.getTransactionDate()),
                        escapeCSV(t.getMemberName() != null ? t.getMemberName() : "N/A"),
                        t.getDebitCredit(),
                        t.getTransactionType().name(),
                        escapeCSV(t.getDescription() != null ? t.getDescription() : ""),
                        formatAmount(t.getAmount(), t.getDebitCredit())
                ));
            }

            writer.flush();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Failed to export to CSV", e);
            throw new BusinessException("Failed to export transactions to CSV");
        }
    }

    /**
     * Export transactions to Excel
     */
    public byte[] exportToExcel(UUID groupId, UUID memberId, String type, String search) {
        List<TransactionResponse> transactions = transactionService.getTransactionsForExport(groupId, memberId, type, search);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Transactions");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Create amount styles
            CellStyle creditStyle = workbook.createCellStyle();
            Font creditFont = workbook.createFont();
            creditFont.setColor(IndexedColors.GREEN.getIndex());
            creditStyle.setFont(creditFont);

            CellStyle debitStyle = workbook.createCellStyle();
            Font debitFont = workbook.createFont();
            debitFont.setColor(IndexedColors.RED.getIndex());
            debitStyle.setFont(debitFont);

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            int rowNum = 1;
            for (TransactionResponse t : transactions) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(t.getTransactionNumber());
                row.createCell(1).setCellValue(formatDate(t.getTransactionDate()));
                row.createCell(2).setCellValue(t.getMemberName() != null ? t.getMemberName() : "N/A");
                row.createCell(3).setCellValue(t.getDebitCredit());
                row.createCell(4).setCellValue(t.getTransactionType().name());
                row.createCell(5).setCellValue(t.getDescription() != null ? t.getDescription() : "");

                org.apache.poi.ss.usermodel.Cell amountCell = row.createCell(6);
                amountCell.setCellValue(formatAmount(t.getAmount(), t.getDebitCredit()));
                amountCell.setCellStyle("CREDIT".equals(t.getDebitCredit()) ? creditStyle : debitStyle);
            }

            // Auto-size columns
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Add summary
            int summaryRow = rowNum + 2;
            BigDecimal totalCredits = transactions.stream()
                    .filter(t -> "CREDIT".equals(t.getDebitCredit()))
                    .map(TransactionResponse::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalDebits = transactions.stream()
                    .filter(t -> "DEBIT".equals(t.getDebitCredit()))
                    .map(TransactionResponse::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Row creditSumRow = sheet.createRow(summaryRow);
            creditSumRow.createCell(5).setCellValue("Total Credits:");
            org.apache.poi.ss.usermodel.Cell creditSumCell = creditSumRow.createCell(6);
            creditSumCell.setCellValue("KES " + totalCredits.toPlainString());
            creditSumCell.setCellStyle(creditStyle);

            Row debitSumRow = sheet.createRow(summaryRow + 1);
            debitSumRow.createCell(5).setCellValue("Total Debits:");
            org.apache.poi.ss.usermodel.Cell debitSumCell = debitSumRow.createCell(6);
            debitSumCell.setCellValue("KES " + totalDebits.toPlainString());
            debitSumCell.setCellStyle(debitStyle);

            Row netRow = sheet.createRow(summaryRow + 2);
            netRow.createCell(5).setCellValue("Net Balance:");
            netRow.createCell(6).setCellValue("KES " + totalCredits.subtract(totalDebits).toPlainString());

            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Failed to export to Excel", e);
            throw new BusinessException("Failed to export transactions to Excel");
        }
    }

    /**
     * Export transactions to PDF
     */
    public byte[] exportToPdf(UUID groupId, UUID memberId, String type, String search) {
        List<TransactionResponse> transactions = transactionService.getTransactionsForExport(groupId, memberId, type, search);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4.rotate());

            // Title
            Paragraph title = new Paragraph("Transaction Report")
                    .setFontSize(18)
                    .simulateBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);
            document.add(title);

            // Generated date
            Paragraph dateInfo = new Paragraph("Generated: " + LocalDateTime.now().format(DATE_FORMAT))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(dateInfo);

            // Create table
            float[] columnWidths = {15, 12, 18, 8, 12, 25, 10};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            // Add headers
            for (String header : HEADERS) {
                Cell cell = new Cell()
                        .add(new Paragraph(header).simulateBold())
                        .setBackgroundColor(ColorConstants.DARK_GRAY)
                        .setFontColor(ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(5);
                table.addHeaderCell(cell);
            }

            // Add data rows
            boolean alternate = false;
            for (TransactionResponse t : transactions) {
                com.itextpdf.kernel.colors.Color bgColor = alternate ?
                        ColorConstants.LIGHT_GRAY : ColorConstants.WHITE;

                table.addCell(createCell(t.getTransactionNumber(), bgColor));
                table.addCell(createCell(formatDate(t.getTransactionDate()), bgColor));
                table.addCell(createCell(t.getMemberName() != null ? t.getMemberName() : "N/A", bgColor));
                table.addCell(createCell(t.getDebitCredit(), bgColor));
                table.addCell(createCell(t.getTransactionType().name(), bgColor));
                table.addCell(createCell(t.getDescription() != null ? t.getDescription() : "", bgColor));

                Cell amountCell = createCell(formatAmount(t.getAmount(), t.getDebitCredit()), bgColor);
                amountCell.setFontColor("CREDIT".equals(t.getDebitCredit()) ?
                        ColorConstants.GREEN : ColorConstants.RED);
                amountCell.setTextAlignment(TextAlignment.RIGHT);
                table.addCell(amountCell);

                alternate = !alternate;
            }

            document.add(table);

            // Add summary
            BigDecimal totalCredits = transactions.stream()
                    .filter(t -> "CREDIT".equals(t.getDebitCredit()))
                    .map(TransactionResponse::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalDebits = transactions.stream()
                    .filter(t -> "DEBIT".equals(t.getDebitCredit()))
                    .map(TransactionResponse::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            document.add(new Paragraph("\n"));

            Table summaryTable = new Table(2);
            summaryTable.setWidth(UnitValue.createPercentValue(30));
            summaryTable.setMarginLeft(500);

            summaryTable.addCell(new Cell().add(new Paragraph("Total Credits:")).simulateBold());
            summaryTable.addCell(new Cell().add(new Paragraph("KES " + totalCredits.toPlainString()))
                    .setFontColor(ColorConstants.GREEN));

            summaryTable.addCell(new Cell().add(new Paragraph("Total Debits:")).simulateBold());
            summaryTable.addCell(new Cell().add(new Paragraph("KES " + totalDebits.toPlainString()))
                    .setFontColor(ColorConstants.RED));

            summaryTable.addCell(new Cell().add(new Paragraph("Net Balance:")).simulateBold());
            summaryTable.addCell(new Cell().add(new Paragraph("KES " + totalCredits.subtract(totalDebits).toPlainString())));

            document.add(summaryTable);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Failed to export to PDF", e);
            throw new BusinessException("Failed to export transactions to PDF");
        }
    }

    private Cell createCell(String content, com.itextpdf.kernel.colors.Color bgColor) {
        return new Cell()
                .add(new Paragraph(content != null ? content : ""))
                .setBackgroundColor(bgColor)
                .setPadding(5)
                .setFontSize(9);
    }

    private String formatDate(Instant instant) {
        if (instant == null) return "";
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(DATE_FORMAT);
    }

    private String formatAmount(BigDecimal amount, String debitCredit) {
        String prefix = "CREDIT".equals(debitCredit) ? "+" : "-";
        return prefix + "KES " + amount.toPlainString();
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}