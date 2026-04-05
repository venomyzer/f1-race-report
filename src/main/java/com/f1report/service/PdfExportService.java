package com.f1report.service;

import com.f1report.dto.DriverResultDTO;
import com.f1report.dto.RaceDataDTO;
import com.f1report.dto.ReportResponseDTO;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PdfExportService – generates a styled PDF race report using OpenPDF.
 *
 * The generated PDF contains:
 *   1. Header banner with race name, season, round, circuit
 *   2. Race summary statistics box (winner, time, fastest lap)
 *   3. Podium section (P1, P2, P3 with team colours)
 *   4. Full results table (all 20 drivers with colour-coded positions)
 *   5. AI-generated journalist report text
 *   6. Footer with generation timestamp
 *
 * OpenPDF API overview:
 *   • Document  – the PDF container (like a blank canvas)
 *   • PdfWriter – writes the Document to a byte stream
 *   • Paragraph – a block of text with Font styling
 *   • PdfPTable – a table with cells, borders, background colours
 *   • PdfPCell  – a single table cell
 *
 * Real-world analogy: PdfExportService is the print shop that takes your
 * race report data and lays it out on paper with proper formatting,
 * tables, and brand colours – just like an F1 team's printed press pack.
 *
 * Colours: We use an F1-inspired dark theme adapted for print:
 *   • Header:   #E10600 (F1 Red)
 *   • Table header: #15151E (F1 Dark)
 *   • P1 row:   #FFD700 (Gold)
 *   • P2 row:   #C0C0C0 (Silver)
 *   • P3 row:   #CD7F32 (Bronze)
 *   • Alt rows: #F5F5F5 (Light grey)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final RaceDataService raceDataService;
    private final ReportService   reportService;

    // ── F1 Brand Colours ──────────────────────────────────────────────────────
    private static final Color F1_RED     = new Color(0xE1, 0x06, 0x00);
    private static final Color F1_DARK    = new Color(0x15, 0x15, 0x1E);
    private static final Color F1_WHITE   = Color.WHITE;
    private static final Color GOLD       = new Color(0xFF, 0xD7, 0x00);
    private static final Color SILVER     = new Color(0xC0, 0xC0, 0xC0);
    private static final Color BRONZE     = new Color(0xCD, 0x7F, 0x32);
    private static final Color ALT_ROW    = new Color(0xF5, 0xF5, 0xF5);
    private static final Color DNF_GREY   = new Color(0xCC, 0xCC, 0xCC);
    private static final Color PURPLE     = new Color(0x9B, 0x00, 0xFF); // Fastest lap

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE      = new Font(Font.HELVETICA, 22, Font.BOLD, F1_WHITE);
    private static final Font FONT_SUBTITLE   = new Font(Font.HELVETICA, 12, Font.NORMAL, F1_WHITE);
    private static final Font FONT_HEADING    = new Font(Font.HELVETICA, 14, Font.BOLD, F1_DARK);
    private static final Font FONT_TABLE_HDR  = new Font(Font.HELVETICA, 9,  Font.BOLD, F1_WHITE);
    private static final Font FONT_TABLE_BODY = new Font(Font.HELVETICA, 9,  Font.NORMAL, F1_DARK);
    private static final Font FONT_BODY       = new Font(Font.HELVETICA, 10, Font.NORMAL, F1_DARK);
    private static final Font FONT_BOLD       = new Font(Font.HELVETICA, 10, Font.BOLD, F1_DARK);
    private static final Font FONT_SMALL      = new Font(Font.HELVETICA, 8,  Font.NORMAL, Color.GRAY);

    // ═════════════════════════════════════════════════════════════════════════
    // PUBLIC METHOD
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Generate a complete PDF for a race report identified by its DB report ID.
     *
     * @param reportId The DB primary key of the Report entity
     * @return PDF bytes ready to send as HTTP response
     */
    public byte[] generatePdf(Long reportId) {
        log.info("Generating PDF for report id={}", reportId);

        // Fetch the stored AI report
        ReportResponseDTO report = reportService.getReportById(reportId);

        // Fetch the race data for the results table
        RaceDataDTO raceData = raceDataService.getRaceData(report.getSeason(), report.getRound());

        try {
            return buildPdf(report, raceData);
        } catch (Exception e) {
            log.error("PDF generation failed for report id={}", reportId, e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Alternative: generate PDF directly from season + round (fetches report if exists).
     */
    public byte[] generatePdfByRace(int season, int round) {
        RaceDataDTO raceData = raceDataService.getRaceData(season, round);

        // Try to get the most recent report for this race
        ReportResponseDTO report = reportService.getReportsBySeason(season)
            .stream()
            .filter(r -> r.getRound().equals(round))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                "No AI report found for S" + season + " R" + round +
                ". Generate a report first via POST /api/generate-report"));

        try {
            return buildPdf(report, raceData);
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PDF BUILDING
    // ═════════════════════════════════════════════════════════════════════════

    private byte[] buildPdf(ReportResponseDTO report, RaceDataDTO raceData) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // A4 page with 36pt margins
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter writer = PdfWriter.getInstance(doc, baos);

        // Add page numbers in footer via page event
        writer.setPageEvent(new PageNumberFooter(report.getRaceName(), report.getSeason()));

        doc.open();

        // ── 1. Header Banner ─────────────────────────────────────────────────
        addHeaderBanner(doc, raceData);

        // ── 2. Summary Stats Box ─────────────────────────────────────────────
        doc.add(Chunk.NEWLINE);
        addSummaryBox(doc, raceData);

        // ── 3. Podium Section ────────────────────────────────────────────────
        doc.add(Chunk.NEWLINE);
        addPodiumSection(doc, raceData);

        // ── 4. Results Table ─────────────────────────────────────────────────
        doc.add(Chunk.NEWLINE);
        addResultsTable(doc, raceData.getResults());

        // ── 5. AI Report Text ────────────────────────────────────────────────
        doc.add(Chunk.NEWLINE);
        addReportText(doc, report);

        // ── 6. Generation Metadata ───────────────────────────────────────────
        addMetadataFooterBox(doc, report);

        doc.close();
        log.info("PDF generated: {} bytes for report id={}", baos.size(), report.getId());
        return baos.toByteArray();
    }

    // ─── Section Builders ─────────────────────────────────────────────────────

    /** Red header banner with race name, circuit, date */
    private void addHeaderBanner(Document doc, RaceDataDTO raceData) throws Exception {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBackgroundColor(F1_RED);
        titleCell.setPadding(18);
        titleCell.setBorder(Rectangle.NO_BORDER);

        titleCell.addElement(new Paragraph("F1 RACE REPORT", FONT_TITLE));
        titleCell.addElement(new Paragraph(raceData.getRaceName().toUpperCase(), FONT_TITLE));
        titleCell.addElement(new Paragraph(
            String.format("Season %d | Round %d | %s",
                raceData.getSeason(), raceData.getRound(), raceData.getRaceDate()),
            FONT_SUBTITLE));
        titleCell.addElement(new Paragraph(
            String.format("%s, %s, %s",
                raceData.getCircuitName(), raceData.getLocality(), raceData.getCountry()),
            FONT_SUBTITLE));

        banner.addCell(titleCell);
        doc.add(banner);
    }

    /** Grey summary stats box: winner, time, fastest lap, retirements */
    private void addSummaryBox(Document doc, RaceDataDTO raceData) throws Exception {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 1, 1, 1});

        addSummaryStat(table, "WINNER", raceData.getWinnerName()
            + "\n" + raceData.getWinnerConstructor());
        addSummaryStat(table, "RACE TIME", raceData.getWinnerTime() != null
            ? raceData.getWinnerTime() : "N/A");
        addSummaryStat(table, "FASTEST LAP", raceData.getFastestLapHolder() != null
            ? raceData.getFastestLapHolder() + "\n" + raceData.getFastestLapTime()
            : "N/A");
        addSummaryStat(table, "RETIREMENTS",
            raceData.getRetirements() + " DNF(s)\n" +
            raceData.getClassifiedFinishers() + " classified");

        doc.add(table);
    }

    private void addSummaryStat(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(F1_DARK);
        cell.setPadding(10);
        cell.setBorderColor(F1_RED);
        cell.setBorderWidth(1f);
        cell.addElement(new Paragraph(label, FONT_SMALL));
        cell.addElement(new Paragraph(value != null ? value : "-",
            new Font(Font.HELVETICA, 10, Font.BOLD, F1_WHITE)));
        table.addCell(cell);
    }

    /** Podium P1/P2/P3 section with gold/silver/bronze colours */
    private void addPodiumSection(Document doc, RaceDataDTO raceData) throws Exception {
        if (raceData.getPodium() == null || raceData.getPodium().isEmpty()) return;

        Paragraph heading = new Paragraph("PODIUM", FONT_HEADING);
        heading.setSpacingBefore(8);
        heading.setSpacingAfter(6);
        doc.add(heading);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);

        Color[] podiumColors = {GOLD, SILVER, BRONZE};
        String[] medals      = {"🥇 P1", "🥈 P2", "🥉 P3"};

        List<DriverResultDTO> podium = raceData.getPodium();
        for (int i = 0; i < Math.min(podium.size(), 3); i++) {
            DriverResultDTO d = podium.get(i);
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(podiumColors[i]);
            cell.setPadding(10);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.addElement(new Paragraph(medals[i],
                new Font(Font.HELVETICA, 10, Font.BOLD, F1_DARK)));
            cell.addElement(new Paragraph(d.getDriverName(),
                new Font(Font.HELVETICA, 11, Font.BOLD, F1_DARK)));
            cell.addElement(new Paragraph(d.getConstructorName(),
                new Font(Font.HELVETICA, 9, Font.NORMAL, F1_DARK)));
            cell.addElement(new Paragraph(
                "Points: " + (int) d.getPoints().doubleValue(),
                new Font(Font.HELVETICA, 9, Font.NORMAL, F1_DARK)));
            table.addCell(cell);
        }
        // Fill remaining cells if podium has < 3 (edge case for incomplete data)
        for (int i = podium.size(); i < 3; i++) {
            PdfPCell empty = new PdfPCell(new Phrase("N/A"));
            empty.setBorder(Rectangle.NO_BORDER);
            table.addCell(empty);
        }

        doc.add(table);
    }

    /** Full 20-driver results table with alternating rows and colour-coded positions */
    private void addResultsTable(Document doc, List<DriverResultDTO> results) throws Exception {
        if (results == null || results.isEmpty()) return;

        Paragraph heading = new Paragraph("RACE RESULTS", FONT_HEADING);
        heading.setSpacingBefore(8);
        heading.setSpacingAfter(6);
        doc.add(heading);

        // Columns: POS | DRIVER | TEAM | GRID | LAPS | STATUS | PTS | FASTEST
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.5f, 2f, 1.5f, 0.6f, 0.6f, 1.2f, 0.6f, 1.2f});

        // Header row
        String[] headers = {"POS", "DRIVER", "TEAM", "GRID", "LAPS", "STATUS", "PTS", "FASTEST LAP"};
        for (String h : headers) {
            PdfPCell hCell = new PdfPCell(new Phrase(h, FONT_TABLE_HDR));
            hCell.setBackgroundColor(F1_DARK);
            hCell.setPadding(6);
            hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(hCell);
        }

        // Data rows
        for (int i = 0; i < results.size(); i++) {
            DriverResultDTO r = results.get(i);
            Color rowColor    = getRowColor(r, i);

            addTableCell(table, r.getFinishingPosition() != null
                ? String.valueOf(r.getFinishingPosition()) : "–", rowColor, Element.ALIGN_CENTER);
            addTableCell(table, r.getDriverCode() + "  " + r.getDriverName(), rowColor, Element.ALIGN_LEFT);
            addTableCell(table, r.getConstructorName(), rowColor, Element.ALIGN_LEFT);
            addTableCell(table, r.getGridPosition() != null
                ? String.valueOf(r.getGridPosition()) : "–", rowColor, Element.ALIGN_CENTER);
            addTableCell(table, r.getLapsCompleted() != null
                ? String.valueOf(r.getLapsCompleted()) : "–", rowColor, Element.ALIGN_CENTER);
            addTableCell(table, r.getStatus(), rowColor, Element.ALIGN_LEFT);
            addTableCell(table, r.getPoints() != null
                ? String.valueOf(r.getPoints().intValue()) : "0", rowColor, Element.ALIGN_CENTER);

            // Fastest lap cell – purple background if this driver set fastest lap
            PdfPCell flCell = new PdfPCell(new Phrase(
                r.getFastestLapTime() != null ? r.getFastestLapTime() : "–", FONT_TABLE_BODY));
            flCell.setPadding(5);
            flCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            flCell.setBackgroundColor(r.hasFastestLap() ? PURPLE : rowColor);
            if (r.hasFastestLap()) {
                flCell.setPhrase(new Phrase(r.getFastestLapTime(),
                    new Font(Font.HELVETICA, 9, Font.BOLD, F1_WHITE)));
            }
            table.addCell(flCell);
        }

        doc.add(table);
    }

    /** AI-generated report text section */
    private void addReportText(Document doc, ReportResponseDTO report) throws Exception {
        Paragraph heading = new Paragraph("AI RACE REPORT", FONT_HEADING);
        heading.setSpacingBefore(12);
        heading.setSpacingAfter(8);
        doc.add(heading);

        // Add a red separator line
        PdfPTable separator = new PdfPTable(1);
        separator.setWidthPercentage(100);
        PdfPCell sepCell = new PdfPCell();
        sepCell.setBackgroundColor(F1_RED);
        sepCell.setFixedHeight(3);
        sepCell.setBorder(Rectangle.NO_BORDER);
        separator.addCell(sepCell);
        doc.add(separator);
        doc.add(Chunk.NEWLINE);

        // Render the markdown-formatted report as plain text paragraphs
        // (Full markdown rendering would require a 3rd party library)
        String[] lines = report.getReportContent().split("\n");
        for (String line : lines) {
            if (line.isBlank()) {
                doc.add(Chunk.NEWLINE);
                continue;
            }
            // Bold section headers (lines starting with **)
            if (line.startsWith("**") && line.endsWith("**")) {
                String title = line.replaceAll("\\*\\*", "");
                Paragraph p = new Paragraph(title, FONT_BOLD);
                p.setSpacingBefore(8);
                doc.add(p);
            } else {
                // Strip remaining markdown bold markers from inline bold text
                String cleaned = line.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
                doc.add(new Paragraph(cleaned, FONT_BODY));
            }
        }
    }

    /** Small metadata box at the end: model used, token count, generated time */
    private void addMetadataFooterBox(Document doc, ReportResponseDTO report) throws Exception {
        doc.add(Chunk.NEWLINE);
        PdfPTable meta = new PdfPTable(1);
        meta.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(ALT_ROW);
        cell.setPadding(8);
        cell.setBorderColor(Color.LIGHT_GRAY);

        String timestamp = report.getGeneratedAt() != null
            ? report.getGeneratedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss"))
            : "Unknown";

        cell.addElement(new Paragraph(
            String.format("Generated by: %s  |  Tokens: %d prompt + %d completion  |  %s",
                report.getModelUsed() != null ? report.getModelUsed() : "AI",
                report.getPromptTokens() != null ? report.getPromptTokens() : 0,
                report.getCompletionTokens() != null ? report.getCompletionTokens() : 0,
                timestamp),
            FONT_SMALL));
        meta.addCell(cell);
        doc.add(meta);
    }

    // ── Utility methods ───────────────────────────────────────────────────────

    private void addTableCell(PdfPTable table, String text, Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "–", FONT_TABLE_BODY));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    /** Returns the background colour for a result row based on finishing position */
    private Color getRowColor(DriverResultDTO r, int rowIndex) {
        if (r.getFinishingPosition() == null) return DNF_GREY;
        return switch (r.getFinishingPosition()) {
            case 1  -> GOLD;
            case 2  -> SILVER;
            case 3  -> BRONZE;
            default -> rowIndex % 2 == 0 ? Color.WHITE : ALT_ROW;
        };
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PAGE EVENT: Add page numbers and footer to every page
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * PageNumberFooter – adds "Race Name | Season | Page X of Y" to every page.
     * Implements PdfPageEventHelper (the OpenPDF way to add running headers/footers).
     *
     * Real-world analogy: like the footer automatically printed on every page
     * of a legal document – the printer handles it, not the person writing the content.
     */
    private static class PageNumberFooter extends PdfPageEventHelper {
        private PdfTemplate totalPagesTemplate;
        private final String raceName;
        private final int    season;

        PageNumberFooter(String raceName, int season) {
            this.raceName = raceName;
            this.season   = season;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            // Placeholder that will be filled in with total page count at close
            totalPagesTemplate = writer.getDirectContent().createTemplate(30, 16);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Font footerFont   = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);

            // Left: race name + season
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(raceName + " | Season " + season, footerFont),
                document.leftMargin(), document.bottom() - 10, 0);

            // Right: Page N of M
            Phrase pagePhrase = new Phrase("Page " + writer.getPageNumber() + " of ", footerFont);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                pagePhrase,
                document.right() - 30, document.bottom() - 10, 0);

            cb.addTemplate(totalPagesTemplate,
                document.right() - 28, document.bottom() - 11);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            // Fill in the total page count placeholder
            ColumnText.showTextAligned(
                new PdfCanvas(totalPagesTemplate),
                Element.ALIGN_LEFT,
                new Phrase(String.valueOf(writer.getPageNumber() - 1),
                    new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY)),
                0, 2, 0);
        }

        // PdfCanvas is a minimal wrapper since OpenPDF's template API needs it
        private static class PdfCanvas extends PdfContentByte {
            PdfCanvas(PdfTemplate template) { super(null); }
        }
    }
}
