package com.f1report.controller;

import com.f1report.dto.ApiResponseDTO;
import com.f1report.dto.RaceDataDTO;
import com.f1report.service.RaceDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * RaceController – handles the main race data endpoint.
 *
 * Endpoint:
 *   GET /api/race-data?season=YYYY&round=X
 *
 * Returns the complete RaceDataDTO payload:
 *   • Race metadata (name, circuit, date)
 *   • All 20 driver results
 *   • Podium (top 3)
 *   • Lap-by-lap position data (for line chart)
 *   • Driver championship standings (for bar chart)
 *   • Summary stats (winner, fastest lap, retirements)
 *
 * This is the heaviest endpoint – it may trigger calls to:
 *   • Ergast API (race results + lap data + standings)
 *   • PostgreSQL (if results are cached there)
 * All caching is handled transparently by @Cacheable in ErgastService.
 *
 * Response time:
 *   • Cache hit (Caffeine)  → <50ms
 *   • Cache hit (PostgreSQL)→ ~100ms
 *   • Cache miss (Ergast)   → 1–4 seconds (3 external API calls)
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RaceController {

    private final RaceDataService raceDataService;

    /**
     * GET /api/race-data?season=2024&round=1
     *
     * Example success response (truncated):
     * {
     *   "success": true,
     *   "data": {
     *     "season": 2024,
     *     "round": 1,
     *     "raceName": "Bahrain Grand Prix",
     *     "winnerName": "Max Verstappen",
     *     "winnerConstructor": "Red Bull",
     *     "results": [ ... 20 driver results ... ],
     *     "podium": [ ... top 3 ... ],
     *     "lapData": { "lapPoints": [...], "driverCodes": ["VER","PER",...] },
     *     "driverStandings": [ ... ]
     *   }
     * }
     *
     * @param season The championship year
     * @param round  The race round number within the season
     */
    @GetMapping("/race-data")
    public ResponseEntity<ApiResponseDTO<RaceDataDTO>> getRaceData(
            @RequestParam(name = "season", required = true) int season,
            @RequestParam(name = "round",  required = true) int round) {

        log.info("GET /api/race-data?season={}&round={}", season, round);

        // Parameter validation
        if (season < 1950 || season > 2099) {
            return ResponseEntity.badRequest()
                .body(ApiResponseDTO.error("Invalid season: " + season, 400));
        }
        if (round < 1 || round > 30) {
            return ResponseEntity.badRequest()
                .body(ApiResponseDTO.error("Invalid round: " + round +
                    " (must be between 1 and 30)", 400));
        }

        try {
            RaceDataDTO raceData = raceDataService.getRaceData(season, round);
            return ResponseEntity.ok(ApiResponseDTO.ok(raceData,
                "Race data loaded for " + raceData.getRaceName()));

        } catch (RuntimeException e) {
            // Race data not found (future race, wrong round, etc.)
            if (e.getMessage() != null && e.getMessage().contains("No race data found")) {
                return ResponseEntity.status(404)
                    .body(ApiResponseDTO.error(e.getMessage(), 404));
            }
            log.error("Failed to get race data for S{} R{}", season, round, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponseDTO.error(
                    "Failed to load race data: " + e.getMessage(), 500));
        }
    }
}
