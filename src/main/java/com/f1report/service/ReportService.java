package com.f1report.service;

import com.f1report.dto.RaceDataDTO;
import com.f1report.dto.ReportRequestDTO;
import com.f1report.dto.ReportResponseDTO;
import com.f1report.model.Report;
import com.f1report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ReportService – manages the full lifecycle of AI race reports.
 *
 * Flow for POST /api/generate-report:
 *
 *   ┌──────────────────────────────────────────────────────┐
 *   │ 1. Check DB: does a report exist for this race?      │
 *   │    └─ YES + forceRegenerate=false → return from DB   │
 *   │    └─ NO or forceRegenerate=true  → continue         │
 *   │ 2. Fetch full RaceDataDTO (ErgastService)             │
 *   │ 3. Call GroqService to generate AI report             │
 *   │ 4. Persist the report to DB                          │
 *   │ 5. Return ReportResponseDTO                          │
 *   └──────────────────────────────────────────────────────┘
 *
 * Real-world analogy: like a photo printing shop.
 * If you've already printed that photo before, we hand you the existing print.
 * If it's a new photo (or you want a fresh print), we print a new one,
 * keep a copy in the archive, and hand you the result.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final RaceDataService  raceDataService;
    private final GroqService      groqService;

    // ═════════════════════════════════════════════════════════════════════════
    // GENERATE REPORT
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Generate (or retrieve) an AI race report.
     *
     * @param request Contains season, round, forceRegenerate flag
     * @return ReportResponseDTO with the report text + metadata
     */
    @Transactional
    public ReportResponseDTO generateReport(ReportRequestDTO request) {
        int season = request.getSeason();
        int round  = request.getRound();

        // ── Step 1: Check for existing report in DB ───────────────────────────
        if (!request.isForceRegenerate()) {
            Optional<Report> existing = reportRepository.findLatestBySeasonAndRound(season, round);
            if (existing.isPresent()) {
                log.info("Returning cached report for S{} R{} (id={})",
                    season, round, existing.get().getId());
                return toResponseDTO(existing.get(), true);
            }
        }

        log.info("Generating fresh AI report for S{} R{}", season, round);

        // ── Step 2: Fetch race data ────────────────────────────────────────────
        RaceDataDTO raceData;
        try {
            raceData = raceDataService.getRaceData(season, round);
        } catch (Exception e) {
            throw new RuntimeException(
                String.format("Cannot generate report: race data unavailable for S%d R%d. %s",
                    season, round, e.getMessage()), e);
        }

        // ── Step 3: Generate AI report via Groq ───────────────────────────────
        GroqService.GroqResponse groqResponse;
        try {
            groqResponse = groqService.generateRaceReport(raceData);
        } catch (Exception e) {
            throw new RuntimeException("AI report generation failed: " + e.getMessage(), e);
        }

        // ── Step 4: Persist report to DB ──────────────────────────────────────
        Report savedReport = reportRepository.save(Report.builder()
            .season(season)
            .round(round)
            .raceName(raceData.getRaceName())
            .reportContent(groqResponse.reportText())
            .modelUsed(groqResponse.modelUsed())
            .promptTokens(groqResponse.promptTokens())
            .completionTokens(groqResponse.completionTokens())
            .winnerName(raceData.getWinnerName())
            .winnerConstructor(raceData.getWinnerConstructor())
            .build());

        log.info("Report saved to DB: id={}, tokens={}", savedReport.getId(),
            groqResponse.promptTokens() + groqResponse.completionTokens());

        // ── Step 5: Return DTO ────────────────────────────────────────────────
        return toResponseDTO(savedReport, false);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // READ OPERATIONS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Get a specific report by its DB ID.
     * Used by the PDF export endpoint to retrieve the report content.
     */
    public ReportResponseDTO getReportById(Long id) {
        Report report = reportRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Report not found with id: " + id));
        return toResponseDTO(report, true);
    }

    /**
     * Get the latest 10 reports across all seasons (for dashboard widget).
     */
    public List<ReportResponseDTO> getRecentReports() {
        return reportRepository.findTop10RecentReports()
            .stream()
            .map(r -> toResponseDTO(r, true))
            .collect(Collectors.toList());
    }

    /**
     * Get all reports for a specific season.
     */
    public List<ReportResponseDTO> getReportsBySeason(int season) {
        return reportRepository.findBySeasonOrderByRoundAsc(season)
            .stream()
            .map(r -> toResponseDTO(r, true))
            .collect(Collectors.toList());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MAPPER
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Map a Report entity to a ReportResponseDTO.
     *
     * @param report    The JPA entity from the DB
     * @param fromCache Whether this was retrieved from DB (true) or freshly generated (false)
     */
    private ReportResponseDTO toResponseDTO(Report report, boolean fromCache) {
        return ReportResponseDTO.builder()
            .id(report.getId())
            .season(report.getSeason())
            .round(report.getRound())
            .raceName(report.getRaceName())
            .reportContent(report.getReportContent())
            .modelUsed(report.getModelUsed())
            .promptTokens(report.getPromptTokens())
            .completionTokens(report.getCompletionTokens())
            .generatedAt(report.getCreatedAt())
            .fromCache(fromCache)
            .winnerName(report.getWinnerName())
            .winnerConstructor(report.getWinnerConstructor())
            .build();
    }
}
