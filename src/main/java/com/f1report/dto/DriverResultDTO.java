package com.f1report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DriverResultDTO – represents a single driver's finishing data in a race.
 *
 * This DTO feeds two frontend components:
 *   1. The Results Table (all fields shown in a grid)
 *   2. The Final Standings Bar Chart (position + driverName + points)
 *
 * It is a "flattened" view of the RaceResult entity:
 *   RaceResult has @ManyToOne Driver (a separate object with nested fields).
 *   DriverResultDTO merges those into one flat object so React doesn't need
 *   to do rr.driver.familyName – it just reads rr.driverName directly.
 *
 * Real-world analogy: like a spreadsheet row where each column is a specific
 * piece of driver data, rather than a nested folder hierarchy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverResultDTO {

    // ── Driver identity fields (flattened from Driver entity) ────────────────
    private String  driverId;           // "max_verstappen"
    private String  driverCode;         // "VER"
    private String  driverName;         // "Max Verstappen"
    private String  nationality;        // "Dutch"
    private String  constructorName;    // "Red Bull"
    private String  carNumber;          // "1"

    // ── Race result fields ───────────────────────────────────────────────────
    private Integer finishingPosition;  // 1, 2, 3 ... 20, null if DNF
    private Integer gridPosition;       // Starting position on the grid
    private Double  points;             // 25.0, 18.0, 15.0 ... 0.0
    private Integer lapsCompleted;      // Number of laps finished
    private String  status;             // "Finished", "+1 Lap", "DNF", "DSQ"
    private String  finishTime;         // "1:33:56.736" (winner) or "+5.250"

    // ── Fastest lap ──────────────────────────────────────────────────────────
    private String  fastestLapTime;     // "1:32.608"
    private Integer fastestLapRank;     // 1 = set the overall fastest lap
    private Double  fastestLapAvgSpeed; // 206.018 (kph)

    // ── Computed helpers for frontend display ────────────────────────────────

    /**
     * Returns a medal emoji for top 3 positions.
     * Useful for the podium display component.
     * e.g., position 1 → "🥇", 2 → "🥈", 3 → "🥉", others → ""
     */
    public String getMedalEmoji() {
        if (finishingPosition == null) return "";
        return switch (finishingPosition) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "";
        };
    }

    /**
     * Returns true if this driver set the race's fastest lap.
     * Used to render a purple highlight (F1 broadcast convention).
     */
    public boolean hasFastestLap() {
        return fastestLapRank != null && fastestLapRank == 1;
    }

    /**
     * Returns true if the driver finished the race (not DNF/DSQ).
     * "Finished" or "+X Lap(s)" both count as classified finishes.
     */
    public boolean isClassifiedFinish() {
        return status != null &&
               (status.equals("Finished") || status.startsWith("+"));
    }
}
