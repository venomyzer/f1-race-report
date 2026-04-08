package com.f1report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RaceDataDTO – the master response payload for GET /api/race-data.
 *
 * This is the single "super-response" that gives the frontend everything
 * it needs to render the full results page in one API call:
 *   • Race metadata (name, circuit, date)
 *   • Full results table (all 20 drivers, positions, points, times)
 *   • Lap data (for the position-vs-lap line chart)
 *   • Podium data (top 3)
 *   • Driver standings (for the championship bar chart)
 *   • Summary stats (winner, fastest lap holder, total laps, etc.)
 *
 * Real-world analogy: this is the "race weekend dossier" a team principal
 * receives after a race – everything compiled into one document.
 *
 * Design note: we deliberately bundle all data into one response (not
 * separate /results, /laps, /standings endpoints) to avoid the frontend
 * making 3–4 parallel requests and managing complex async state.
 * One request → one loading state → one render.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceDataDTO {

    // ── Race Identity ────────────────────────────────────────────────────────
    private Integer season;
    private Integer round;
    private String  raceName;        // "Monaco Grand Prix"
    private String  circuitName;     // "Circuit de Monaco"
    private String  country;         // "Monaco"
    private String  locality;        // "Monte-Carlo"
    private String  raceDate;        // "2024-05-26"

    // ── Full Results (20 driver rows) ────────────────────────────────────────
    /**
     * All driver results sorted by finishing position.
     * Feeds: the results table component + the standings bar chart.
     */
    private List<DriverResultDTO> results;

    // ── Podium ───────────────────────────────────────────────────────────────
    /** First 3 results from the results list, extracted for the podium widget */
    private List<DriverResultDTO> podium;

    // ── Lap Data (for line chart) ────────────────────────────────────────────
    /**
     * Transposed, chart-ready lap data.
     * Null or lapDataAvailable=false if the API didn't have lap data.
     */
    private LapDataDTO lapData;

    // ── Summary Stats ────────────────────────────────────────────────────────

    /** Winner's full name: "Max Verstappen" */
    private String  winnerName;

    /** Winner's constructor: "Red Bull" */
    private String  winnerConstructor;

    /** Winner's race time: "1:33:56.736" */
    private String  winnerTime;

    /** Name of the driver who set the fastest lap */
    private String  fastestLapHolder;

    /** The fastest lap time: "1:32.608" */
    private String  fastestLapTime;

    /** Total laps completed in the race */
    private Integer totalLaps;

    /**
     * Number of classified finishers (status = Finished or +X Lap).
     * Useful for the frontend to show "19 of 20 drivers classified".
     */
    private Integer classifiedFinishers;

    /**
     * Number of retirements (DNF/DNQ/DSQ).
     * Useful for race summary narrative.
     */
    private Integer retirements;

    // ── Driver Championship Standings (after this race) ──────────────────────
    /**
     * Championship standings as of this race round.
     * Feeds the championship standings bar chart.
     * Each entry: { driverCode, driverName, points, position }
     */
    private List<StandingDTO> driverStandings;

    // ── Inner DTO for standings ──────────────────────────────────────────────
    /**
     * StandingDTO – a single driver's championship standing.
     * Nested inner class so it stays logically grouped with RaceDataDTO.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StandingDTO {
        private Integer position;
        private String  driverId;
        private String  driverCode;
        private String  driverName;
        private String  constructorName;
        private Double  points;
        private Integer wins;
    }
}
