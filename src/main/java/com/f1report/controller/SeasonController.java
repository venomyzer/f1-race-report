package com.f1report.controller;

import com.f1report.dto.ApiResponseDTO;
import com.f1report.dto.RaceDTO;
import com.f1report.dto.SeasonDTO;
import com.f1report.service.ErgastService;
import com.f1report.service.RaceDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SeasonController – handles HTTP requests for seasons and race lists.
 *
 * Endpoints:
 *   GET /api/seasons            → List of all F1 seasons (1950–present)
 *   GET /api/races?season=YYYY  → List of races in a given season
 *
 * Controller responsibilities (ONLY):
 *   1. Accept the HTTP request and parse parameters
 *   2. Delegate to the appropriate Service method
 *   3. Wrap the result in ApiResponseDTO and return ResponseEntity
 *   4. Handle exceptions with meaningful HTTP status codes
 *
 * Controllers should contain ZERO business logic.
 * Real-world analogy: a controller is a receptionist – they take your request,
 * pass it to the right department (service), and bring back the result.
 * They don't actually do the work themselves.
 *
 * @RestController = @Controller + @ResponseBody
 *   → every method return value is serialised to JSON automatically by Jackson
 *
 * @RequestMapping("/api") = all endpoints in this class are prefixed with /api
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SeasonController {

    private final ErgastService   ergastService;
    private final RaceDataService raceDataService;

    /**
     * GET /api/seasons
     *
     * Returns a list of all available F1 seasons, sorted newest first.
     * The frontend uses this to populate the "Select Season" dropdown.
     *
     * Example response:
     * {
     *   "success": true,
     *   "message": "OK",
     *   "data": [
     *     { "season": 2024, "url": "https://en.wikipedia.org/wiki/2024_Formula_One_World_Championship" },
     *     { "season": 2023, "url": "..." },
     *     ...
     *   ],
     *   "statusCode": 200
     * }
     *
     * ResponseEntity<ApiResponseDTO<T>> gives us full control over:
     *   • HTTP status code (200, 500, etc.)
     *   • Response headers (Content-Type is set automatically)
     *   • Response body (our wrapped DTO)
     */
    @GetMapping("/seasons")
    public ResponseEntity<ApiResponseDTO<List<SeasonDTO>>> getSeasons() {
        log.info("GET /api/seasons");
        try {
            List<SeasonDTO> seasons = ergastService.fetchSeasons();
            return ResponseEntity.ok(ApiResponseDTO.ok(seasons,
                "Fetched " + seasons.size() + " seasons"));
        } catch (Exception e) {
            log.error("Failed to get seasons", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponseDTO.error("Failed to fetch seasons: " + e.getMessage(), 500));
        }
    }

    /**
     * GET /api/races?season=2024
     *
     * Returns all races for a given F1 season.
     * The frontend uses this to populate the "Select Race" dropdown
     * after the user picks a season.
     *
     * @param season The F1 championship year (e.g., 2024)
     *               @RequestParam = maps the "?season=2024" query parameter
     *               required = true → Spring returns 400 if omitted
     *
     * Example response:
     * {
     *   "success": true,
     *   "data": [
     *     {
     *       "season": 2024, "round": 1, "raceName": "Bahrain Grand Prix",
     *       "circuitName": "Bahrain International Circuit",
     *       "country": "Bahrain", "raceDate": "2024-03-02"
     *     },
     *     ...
     *   ]
     * }
     */
    @GetMapping("/races")
    public ResponseEntity<ApiResponseDTO<List<RaceDTO>>> getRacesBySeason(
            @RequestParam(name = "season", required = true) int season) {

        log.info("GET /api/races?season={}", season);

        // Basic validation: F1 started in 1950; allow up to next year
        if (season < 1950 || season > 2099) {
            return ResponseEntity.badRequest()
                .body(ApiResponseDTO.error("Invalid season: must be between 1950 and 2099", 400));
        }

        try {
            List<RaceDTO> races = raceDataService.getRacesForSeason(season);
            if (races.isEmpty()) {
                return ResponseEntity.ok(ApiResponseDTO.ok(races,
                    "No races found for season " + season + " (season may not have started yet)"));
            }
            return ResponseEntity.ok(ApiResponseDTO.ok(races,
                "Fetched " + races.size() + " races for season " + season));
        } catch (Exception e) {
            log.error("Failed to get races for season {}", season, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponseDTO.error(
                    "Failed to fetch races for season " + season + ": " + e.getMessage(), 500));
        }
    }
}
