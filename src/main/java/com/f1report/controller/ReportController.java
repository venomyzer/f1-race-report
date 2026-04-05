package com.f1report.controller;

import com.f1report.dto.ApiResponseDTO;
import com.f1report.dto.ReportRequestDTO;
import com.f1report.dto.ReportResponseDTO;
import com.f1report.service.PdfExportService;
import com.f1report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ReportController – handles all AI report generation and PDF export endpoints.
 *
 * Endpoints:
 *   POST /api/generate-report       → Generate (or retrieve) an AI race report
 *   GET  /api/reports               → List recent reports (dashboard widget)
 *   GET  /api/reports/season/{year} → All reports for a specific season
 *   GET  /api/reports/{id}          → Get a specific report by ID
 *   GET  /api/export-pdf/{id}       → Download report as PDF
 *   GET  /api/export-pdf/race       → Download PDF by season+round (no DB report ID needed)
 *
 * @Valid on the request body parameter triggers Bean Validation.
 * If the body fails @NotNull/@Min/@Max constraints in ReportRequestDTO,
 * Spring returns 400 Bad Request automatically with field-level error details.
 *
 * Real-world analogy: this controller is the "publications desk" of the F1
 * team. It receives editorial requests (generate report, export PDF),
 * hands them to the editorial team (ReportService, PdfExportService),
 * and delivers the finished publication back to the requester.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService   reportService;
    private final PdfExportService pdfExportService;

    // ═════════════════════════════════════════════════════════════════════════
    // AI REPORT GENERATION
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * POST /api/generate-report
     *
     * Request body:
     * {
     *   "season": 2024,
     *   "round": 5,
     *   "forceRegenerate": false
     * }
     *
     * Response (success):
     * {
     *   "success": true,
     *   "data": {
     *     "id": 42,
     *     "season": 2024,
     *     "round": 5,
     *     "raceName": "Monaco Grand Prix",
     *     "reportContent": "**Monaco Grand Prix Race Report**\n\n**Race Overview**...",
     *     "modelUsed": "llama-3.3-70b-versatile",
     *     "promptTokens": 512,
     *     "completionTokens": 820,
     *     "generatedAt": "2024-06-10T14:32:00",
     *     "fromCache": false,
     *     "winnerName": "Charles Leclerc",
     *     "winnerConstructor": "Ferrari"
     *   }
     * }
     *
     * Note: This can take 5–20 seconds if calling the Groq API fresh.
     *       If fromCache=true, it returns in <100ms from the DB.
     *
     * @param request Validated request body with season, round, forceRegenerate
     */
    @PostMapping("/generate-report")
    public ResponseEntity<ApiResponseDTO<ReportResponseDTO>> generateReport(
            @Valid @RequestBody ReportRequestDTO request) {

        log.info("POST /api/generate-report – S{} R{} forceRegenerate={}",
            request.getSeason(), request.getRound(), request.isForceRegenerate());

        try {
            ReportResponseDTO report = reportService.generateReport(request);

            String message = report.isFromCache()
                ? "Report retrieved from cache (id=" + report.getId() + ")"
                : "AI report generated successfully (id=" + report.getId() + ")";

            return ResponseEntity.ok(ApiResponseDTO.ok(report, message));

        } catch (RuntimeException e) {
            log.error("Report generation failed for S{} R{}",
                request.getSeason(), request.getRound(), e);

            // If race data was not found → 404
            if (e.getMessage() != null && e.getMessage().contains("race data unavailable")) {
                return ResponseEntity.status(404)
                    .body(ApiResponseDTO.error(e.getMessage(), 404));
            }
            // Groq API error → 503 Service Unavailable
            if (e.getMessage() != null && e.getMessage().contains("AI report generation failed")) {
                return ResponseEntity.status(503)
                    .body(ApiResponseDTO.error("AI service unavailable: " + e.getMessage(), 503));
            }
            return ResponseEntity.internalServerError()
                .body(ApiResponseDTO.error("Report generation failed: " + e.getMessage(), 500));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // REPORT RETRIEVAL
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/reports
     * Returns the 10 most recently generated reports for the dashboard.
     */
    @GetMapping("/reports")
    public ResponseEntity<ApiResponseDTO<List<ReportResponseDTO>>> getRecentReports() {
        log.info("GET /api/reports");
        try {
            List<ReportResponseDTO> reports = reportService.getRecentReports();
            return ResponseEntity.ok(ApiResponseDTO.ok(reports,
                "Fetched " + reports.size() + " recent reports"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponseDTO.error("Failed to fetch reports: " + e.getMessage(), 500));
        }
    }

    /**
     * GET /api/reports/season/{season}
     * Returns all reports for a specific season.
     */
    @GetMapping("/reports/season/{season}")
    public ResponseEntity<ApiResponseDTO<List<ReportResponseDTO>>> getReportsBySeason(
            @PathVariable int season) {
        log.info("GET /api/reports/season/{}", season);
        try {
            List<ReportResponseDTO> reports = reportService.getReportsBySeason(season);
            return ResponseEntity.ok(ApiResponseDTO.ok(reports,
                "Fetched " + reports.size() + " reports for season " + season));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponseDTO.error("Failed to fetch reports: " + e.getMessage(), 500));
        }
    }

    /**
     * GET /api/reports/{id}
     * Returns a specific report by its DB primary key.
     */
    @GetMapping("/reports/{id}")
    public ResponseEntity<ApiResponseDTO<ReportResponseDTO>> getReportById(
            @PathVariable Long id) {
        log.info("GET /api/reports/{}", id);
        try {
            ReportResponseDTO report = reportService.getReportById(id);
            return ResponseEntity.ok(ApiResponseDTO.ok(report));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.status(404)
                    .body(ApiResponseDTO.error(e.getMessage(), 404));
            }
            return ResponseEntity.internalServerError()
                .body(ApiResponseDTO.error(e.getMessage(), 500));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PDF EXPORT
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/export-pdf/{id}
     *
     * Downloads the race report with the given DB ID as a PDF file.
     *
     * The browser receives:
     *   Content-Type: application/pdf
     *   Content-Disposition: attachment; filename="f1-report-2024-R5.pdf"
     *
     * MediaType.APPLICATION_PDF → tells the browser this is a PDF.
     * Content-Disposition: attachment → browser downloads it, not renders it in tab.
     * Content-Disposition: inline    → browser would try to display it in the tab.
     *
     * Real-world analogy: like clicking "Download PDF" on a banking statement.
     * The server sends the binary PDF bytes; the browser saves it as a file.
     *
     * @param id The Report entity DB primary key
     */
    @GetMapping("/export-pdf/{id}")
    public ResponseEntity<byte[]> exportPdfById(@PathVariable Long id) {
        log.info("GET /api/export-pdf/{}", id);
        try {
            // First, get the report to know season/round for the filename
            ReportResponseDTO report = reportService.getReportById(id);
            byte[] pdfBytes = pdfExportService.generatePdf(id);

            String filename = String.format("f1-report-%d-R%d.pdf",
                report.getSeason(), report.getRound());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);

        } catch (RuntimeException e) {
            log.error("PDF export failed for report id={}", id, e);
            // Can't return ApiResponseDTO here (body is byte[]), so return 500
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/export-pdf/race?season=2024&round=5
     *
     * Alternative PDF export: by season + round instead of report DB ID.
     * Useful when the frontend has season/round but not the report ID.
     * Requires a report to have been generated first (via POST /api/generate-report).
     */
    @GetMapping("/export-pdf/race")
    public ResponseEntity<byte[]> exportPdfByRace(
            @RequestParam int season,
            @RequestParam int round) {
        log.info("GET /api/export-pdf/race?season={}&round={}", season, round);
        try {
            byte[] pdfBytes = pdfExportService.generatePdfByRace(season, round);
            String filename = String.format("f1-report-%d-R%d.pdf", season, round);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok().headers(headers).body(pdfBytes);

        } catch (RuntimeException e) {
            log.error("PDF export failed for S{} R{}", season, round, e);
            return ResponseEntity.status(
                e.getMessage() != null && e.getMessage().contains("No AI report found")
                    ? 404 : 500
            ).build();
        }
    }
}
