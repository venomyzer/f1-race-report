package com.f1report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * LapDataDTO – structured lap-by-lap position data for the line chart.
 *
 * The Recharts LineChart on the frontend needs data in this shape:
 *   [
 *     { lap: 1, VER: 1, HAM: 3, LEC: 2 },
 *     { lap: 2, VER: 1, HAM: 2, LEC: 3 },
 *     ...
 *   ]
 * Where each key inside a lap entry is a driver code and its value is position.
 *
 * We model this with two complementary structures:
 *   1. LapDataPoint  – one entry per lap: { lapNumber, positions: {VER: 1, HAM: 3} }
 *   2. LapDataDTO    – the full payload: list of data points + driver metadata
 *
 * Real-world analogy: LapDataPoint is like a live leaderboard snapshot taken
 * every lap. LapDataDTO is the full race film reel made up of those snapshots.
 *
 * NOTE: The Ergast lap API returns data per-driver (driver → [lap1, lap2, ...]).
 * We transpose it to per-lap (lap → {driver: position}) for chart compatibility.
 * This transposition happens in ErgastService.parseLapData().
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LapDataDTO {

    /**
     * The transposed, chart-ready lap data.
     * Each map entry: { "lap": 1, "VER": 1, "HAM": 3, "LEC": 2, ... }
     *
     * Using Map<String, Object> so both Integer (lap number, position) and
     * String (driver code) values can coexist in one map without a custom class.
     * Jackson serialises this cleanly to JSON.
     */
    private List<Map<String, Object>> lapPoints;

    /**
     * Ordered list of driver codes that appear in the lap data.
     * The frontend uses this to know which lines to draw on the chart
     * and what colour/label to assign each line.
     *
     * e.g., ["VER", "PER", "HAM", "RUS", "LEC", ...]
     */
    private List<String> driverCodes;

    /**
     * Map from driverCode → full name for chart legend labels.
     * e.g., { "VER": "Max Verstappen", "HAM": "Lewis Hamilton" }
     */
    private Map<String, String> driverNames;

    /**
     * Map from driverCode → constructor name for colour-coding by team.
     * e.g., { "VER": "Red Bull", "PER": "Red Bull", "HAM": "Mercedes" }
     */
    private Map<String, String> driverConstructors;

    /** Total number of laps in the race */
    private Integer totalLaps;

    /** Whether full lap data was available from the API (some old races lack it) */
    private boolean lapDataAvailable;
}
