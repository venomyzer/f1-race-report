package com.f1report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RaceDTO – lightweight race data sent to the frontend for dropdown population.
 *
 * This is intentionally minimal. The frontend only needs enough info to:
 *   1. Display a human-readable label: "Round 1 – Bahrain Grand Prix"
 *   2. Know which round number to pass back when "Generate Report" is clicked.
 *
 * Real-world analogy: a menu item in a restaurant.
 * The full recipe (all race result data) lives in the kitchen (DB + Ergast).
 * The menu just shows the name and number so the customer can order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceDTO {

    private Integer season;
    private Integer round;
    private String  raceName;       // "Bahrain Grand Prix"
    private String  circuitName;    // "Bahrain International Circuit"
    private String  country;        // "Bahrain"
    private String  locality;       // "Sakhir"
    private String  raceDate;       // "2024-03-02" (ISO-8601 string for JSON)
    private String  raceTime;       // "15:00:00Z"

    /** Convenience label for the frontend dropdown: "Round 1 – Bahrain Grand Prix" */
    public String getDropdownLabel() {
        return "Round " + round + " – " + raceName;
    }
}
