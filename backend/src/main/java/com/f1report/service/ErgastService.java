package com.f1report.service;

import com.f1report.config.CacheConfig;
import com.f1report.dto.*;
import com.f1report.dto.RaceDataDTO.StandingDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * ErgastService – integrates with the Jolpica F1 API (successor of Ergast).
 *
 * Responsibilities:
 *   1. Fetch seasons, races, results, lap data, and standings from the API.
 *   2. Parse the deeply nested JSON responses into clean DTOs.
 *   3. Cache results using @Cacheable so repeated calls hit memory, not the API.
 *
 * API base URL: https://api.jolpi.ca/ergast/f1
 * Response format: JSON, same schema as the original Ergast API.
 *
 * All responses are wrapped in "MRData" → nested table → data array.
 * Example: seasons → MRData.SeasonTable.Seasons[{ season, url }]
 *
 * Real-world analogy: ErgastService is like a news correspondent stationed
 * at the F1 paddock. It calls the official F1 data feed (Ergast API),
 * translates the raw technical data into readable briefings (DTOs), and
 * keeps notes (cache) so it doesn't have to re-interview sources for the
 * same questions.
 *
 * @Slf4j      → injects a Logger named `log` (log.info, log.debug, etc.)
 * @Service    → marks this as a Spring-managed bean (singleton by default)
 * @RequiredArgsConstructor → Lombok generates a constructor for all `final` fields,
 *              which Spring uses for dependency injection (constructor injection
 *              is preferred over @Autowired field injection for testability)
 */
@Slf4j
@Service
public class ErgastService {

    // ── Injected dependencies ─────────────────────────────────────────────────

    /**
     * Named qualifier: uses the "ergastRestTemplate" bean from AppConfig,
     * which has the Ergast-specific timeout settings (10s read timeout).
     */
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ErgastService(
            @Qualifier("ergastRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper  = objectMapper;
    }

    // ── Config values from application.properties ─────────────────────────────
    @Value("${ergast.api.base-url}")
    private String baseUrl;

    @Value("${ergast.api.default-limit}")
    private int defaultLimit;

    // ═════════════════════════════════════════════════════════════════════════
    // PUBLIC API METHODS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Fetch all available F1 seasons.
     *
     * @Cacheable("seasons") = before executing this method, Spring checks the
     * "seasons" Caffeine cache. If a cached value exists → return it immediately
     * without calling the API. If not → execute method, store result, return it.
     *
     * The cache key defaults to the method name + all parameters.
     * Since this method has no parameters, the key is always the same →
     * effectively a singleton cache entry for seasons.
     *
     * Ergast endpoint: GET /f1/seasons.json?limit=100&offset=0
     * Returns seasons from 1950 to the current year.
     */
    @Cacheable(CacheConfig.CACHE_SEASONS)
    public List<SeasonDTO> fetchSeasons() {
        String url = baseUrl + "/seasons.json?limit=100";
        log.info("Fetching F1 seasons from Ergast: {}", url);

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root   = objectMapper.readTree(response);
            JsonNode seasons = root.path("MRData")
                                   .path("SeasonTable")
                                   .path("Seasons");

            List<SeasonDTO> result = new ArrayList<>();
            // Iterate over JSON array using StreamSupport (Java 8+ idiom for JsonNode)
            StreamSupport.stream(seasons.spliterator(), false)
                .forEach(node -> result.add(SeasonDTO.builder()
                    .season(node.path("season").asInt())
                    .url(node.path("url").asText(""))
                    .build()));

            // Sort descending (most recent year first)
            result.sort(Comparator.comparing(SeasonDTO::getSeason).reversed());
            log.info("Fetched {} seasons", result.size());
            return result;

        } catch (Exception e) {
            log.error("Failed to fetch seasons from Ergast", e);
            throw new RuntimeException("Failed to fetch F1 seasons: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch all races for a given season.
     *
     * @Cacheable key = the season parameter, so each year is cached separately.
     * Cache entry for 2024 ≠ cache entry for 2023.
     *
     * Ergast endpoint: GET /f1/{season}/races.json
     */
    @Cacheable(value = CacheConfig.CACHE_RACES, key = "#season")
    public List<RaceDTO> fetchRacesBySeason(int season) {
        String url = String.format("%s/%d/races.json?limit=%d", baseUrl, season, defaultLimit);
        log.info("Fetching races for season {} from Ergast: {}", season, url);

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root   = objectMapper.readTree(response);
            JsonNode races  = root.path("MRData")
                                  .path("RaceTable")
                                  .path("Races");

            List<RaceDTO> result = new ArrayList<>();
            races.forEach(node -> result.add(parseRaceNode(node)));
            log.info("Fetched {} races for season {}", result.size(), season);
            return result;

        } catch (Exception e) {
            log.error("Failed to fetch races for season {}", season, e);
            throw new RuntimeException("Failed to fetch races for season " + season, e);
        }
    }

    /**
     * Fetch full race results for a specific race (season + round).
     *
     * Ergast endpoint: GET /f1/{season}/{round}/results.json
     *
     * Returns up to 25 driver results (a full grid is 20 drivers).
     * Cache key = season_round so 2024_1 and 2024_2 are separate entries.
     */
    @Cacheable(value = CacheConfig.CACHE_RACE_RESULTS, key = "#season + '_' + #round")
    public List<DriverResultDTO> fetchRaceResults(int season, int round) {
        String url = String.format("%s/%d/%d/results.json", baseUrl, season, round);
        log.info("Fetching race results for S{} R{}: {}", season, round, url);

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root   = objectMapper.readTree(response);
            JsonNode races  = root.path("MRData")
                                  .path("RaceTable")
                                  .path("Races");

            if (!races.isArray() || races.isEmpty()) {
                log.warn("No results found for S{} R{}", season, round);
                return Collections.emptyList();
            }

            JsonNode resultNodes = races.get(0).path("Results");
            List<DriverResultDTO> results = new ArrayList<>();
            resultNodes.forEach(node -> results.add(parseResultNode(node)));
            log.info("Fetched {} driver results for S{} R{}", results.size(), season, round);
            return results;

        } catch (Exception e) {
            log.error("Failed to fetch results for S{} R{}", season, round, e);
            throw new RuntimeException(
                String.format("Failed to fetch results for Season %d Round %d", season, round), e);
        }
    }

    /**
     * Fetch lap-by-lap timing data and transpose into chart-ready format.
     *
     * Ergast endpoint: GET /f1/{season}/{round}/laps.json?limit=2000
     *
     * Raw Ergast format (per-driver structure):
     *   Laps: [
     *     { number: "1", Timings: [{ driverId: "verstappen", position: "1", time: "1:40.xxx" }] },
     *     { number: "2", Timings: [...] }
     *   ]
     *
     * We TRANSPOSE this to per-lap format for Recharts:
     *   [{ lap: 1, VER: 1, HAM: 3, LEC: 2 }, { lap: 2, VER: 1, HAM: 2, LEC: 3 }]
     *
     * Transposition analogy: the Ergast data is like a spreadsheet where
     * columns are laps and rows are drivers. Recharts needs it rotated 90°
     * so rows are laps and columns are driver codes.
     */
    @Cacheable(value = CacheConfig.CACHE_LAP_DATA, key = "#season + '_' + #round")
    public LapDataDTO fetchLapData(int season, int round) {
        // Ergast limits lap data pagination — we request a high limit to get all laps
        String url = String.format("%s/%d/%d/laps.json?limit=2000", baseUrl, season, round);
        log.info("Fetching lap data for S{} R{}: {}", season, round, url);

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root   = objectMapper.readTree(response);
            JsonNode races  = root.path("MRData")
                                  .path("RaceTable")
                                  .path("Races");

            if (!races.isArray() || races.isEmpty()) {
                log.warn("No lap data available for S{} R{}", season, round);
                return LapDataDTO.builder().lapDataAvailable(false).build();
            }

            JsonNode lapNodes = races.get(0).path("Laps");
            if (lapNodes.isEmpty()) {
                return LapDataDTO.builder().lapDataAvailable(false).build();
            }

            return parseLapData(lapNodes);

        } catch (RestClientException e) {
            log.warn("Lap data not available for S{} R{} – returning empty", season, round);
            return LapDataDTO.builder().lapDataAvailable(false).build();
        } catch (Exception e) {
            log.error("Failed to parse lap data for S{} R{}", season, round, e);
            return LapDataDTO.builder().lapDataAvailable(false).build();
        }
    }

    /**
     * Fetch driver championship standings after a specific race round.
     *
     * Ergast endpoint: GET /f1/{season}/{round}/driverStandings.json
     */
    @Cacheable(value = CacheConfig.CACHE_STANDINGS, key = "#season + '_' + #round")
    public List<StandingDTO> fetchDriverStandings(int season, int round) {
        String url = String.format("%s/%d/%d/driverStandings.json", baseUrl, season, round);
        log.info("Fetching driver standings for S{} R{}: {}", season, round, url);

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root   = objectMapper.readTree(response);
            JsonNode standingLists = root.path("MRData")
                                         .path("StandingsTable")
                                         .path("StandingsLists");

            if (!standingLists.isArray() || standingLists.isEmpty()) {
                return Collections.emptyList();
            }

            JsonNode standingNodes = standingLists.get(0).path("DriverStandings");
            List<StandingDTO> standings = new ArrayList<>();
            standingNodes.forEach(node -> standings.add(parseStandingNode(node)));
            return standings;

        } catch (Exception e) {
            log.warn("Could not fetch standings for S{} R{}: {}", season, round, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PRIVATE PARSING METHODS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Parse a single race JSON node into a RaceDTO.
     * Navigates the Ergast nested structure: Race.Circuit.Location.
     */
    private RaceDTO parseRaceNode(JsonNode node) {
        JsonNode circuit  = node.path("Circuit");
        JsonNode location = circuit.path("Location");

        return RaceDTO.builder()
            .season(node.path("season").asInt())
            .round(node.path("round").asInt())
            .raceName(node.path("raceName").asText())
            .circuitName(circuit.path("circuitName").asText(""))
            .country(location.path("country").asText(""))
            .locality(location.path("locality").asText(""))
            .raceDate(node.path("date").asText(""))
            .raceTime(node.path("time").asText(""))
            .build();
    }

    /**
     * Parse a single driver result JSON node into a DriverResultDTO.
     *
     * Ergast result structure:
     * {
     *   "number": "1",
     *   "position": "1",
     *   "positionText": "1",
     *   "points": "25",
     *   "Driver": { "driverId": "verstappen", "code": "VER", ... },
     *   "Constructor": { "name": "Red Bull", ... },
     *   "grid": "1",
     *   "laps": "57",
     *   "status": "Finished",
     *   "Time": { "millis": "...", "time": "1:33:56.736" },
     *   "FastestLap": { "rank": "1", "lap": "44", "Time": { "time": "1:32.608" }, "AverageSpeed": { "speed": "206.018" } }
     * }
     */
    private DriverResultDTO parseResultNode(JsonNode node) {
        JsonNode driver      = node.path("Driver");
        JsonNode constructor = node.path("Constructor");
        JsonNode timeNode    = node.path("Time");
        JsonNode fastestLap  = node.path("FastestLap");

        // Finishing position: "positionText" can be "R" (retired), "W" (withdrawn)
        // We only parse it as integer if it looks numeric
        String posText   = node.path("positionText").asText("");
        Integer position = posText.matches("\\d+") ? Integer.parseInt(posText) : null;

        // Fastest lap data may be absent (older races, DNF drivers)
        String fastestLapTime    = null;
        Integer fastestLapRank   = null;
        Double fastestLapAvgSpd  = null;
        if (!fastestLap.isMissingNode()) {
            fastestLapTime   = fastestLap.path("Time").path("time").asText(null);
            fastestLapRank   = fastestLap.path("rank").asInt(0);
            String speedStr  = fastestLap.path("AverageSpeed").path("speed").asText(null);
            if (speedStr != null && !speedStr.isEmpty()) {
                fastestLapAvgSpd = Double.parseDouble(speedStr);
            }
        }

        return DriverResultDTO.builder()
            .driverId(driver.path("driverId").asText())
            .driverCode(driver.path("code").asText(""))
            .driverName(driver.path("givenName").asText("")
                      + " " + driver.path("familyName").asText(""))
            .nationality(driver.path("nationality").asText(""))
            .constructorName(constructor.path("name").asText(""))
            .carNumber(node.path("number").asText(""))
            .finishingPosition(position)
            .gridPosition(node.path("grid").asInt(0))
            .points(node.path("points").asDouble(0.0))
            .lapsCompleted(node.path("laps").asInt(0))
            .status(node.path("status").asText(""))
            .finishTime(timeNode.isMissingNode() ? null : timeNode.path("time").asText(null))
            .fastestLapTime(fastestLapTime)
            .fastestLapRank(fastestLapRank)
            .fastestLapAvgSpeed(fastestLapAvgSpd)
            .build();
    }

    /**
     * Parse lap data JSON and TRANSPOSE from per-driver → per-lap.
     *
     * Input structure from Ergast:
     *   Laps: [
     *     { number: "1", Timings: [{ driverId: "verstappen", position: "1" }, ...] },
     *     { number: "2", Timings: [...] }
     *   ]
     *
     * Output we build:
     *   lapPoints: [{ lap: 1, VER: 1, HAM: 3 }, { lap: 2, VER: 1, HAM: 2 }]
     *
     * We also build driverCodes and driverNames for the chart legend.
     */
    private LapDataDTO parseLapData(JsonNode lapNodes) {
        List<Map<String, Object>> lapPoints  = new ArrayList<>();
        Set<String> driverIdsSeen            = new LinkedHashSet<>();   // preserve order
        Map<String, String> driverIdToCode   = new HashMap<>();
        int totalLaps = 0;

        // ── First pass: collect all driverId → code mappings ─────────────────
        lapNodes.forEach(lapNode -> {
            lapNode.path("Timings").forEach(timing -> {
                String driverId = timing.path("driverId").asText();
                driverIdsSeen.add(driverId);
            });
        });

        // ── Second pass: build chart data points ──────────────────────────────
        for (JsonNode lapNode : lapNodes) {
            int lapNumber = lapNode.path("number").asInt();
            totalLaps = Math.max(totalLaps, lapNumber);

            // Map<driverId, position> for this lap
            Map<String, Integer> lapPositions = new HashMap<>();
            lapNode.path("Timings").forEach(timing -> {
                String driverId  = timing.path("driverId").asText();
                int    position  = timing.path("position").asInt();
                lapPositions.put(driverId, position);
            });

            // Build the chart data point: { lap: N, driverId: position, ... }
            Map<String, Object> dataPoint = new LinkedHashMap<>();
            dataPoint.put("lap", lapNumber);
            lapPositions.forEach(dataPoint::put);
            lapPoints.add(dataPoint);
        }

        List<String> driverCodes = new ArrayList<>(driverIdsSeen);

        return LapDataDTO.builder()
            .lapPoints(lapPoints)
            .driverCodes(driverCodes)
            .driverNames(new HashMap<>())       // Enriched later in RaceDataService
            .driverConstructors(new HashMap<>()) // Enriched later in RaceDataService
            .totalLaps(totalLaps)
            .lapDataAvailable(!lapPoints.isEmpty())
            .build();
    }

    /**
     * Parse a single driver standings JSON node into a StandingDTO.
     *
     * Ergast standings structure:
     * {
     *   "position": "1",
     *   "points": "94",
     *   "wins": "4",
     *   "Driver": { "driverId": "verstappen", "code": "VER", ... },
     *   "Constructors": [{ "name": "Red Bull" }]
     * }
     */
    private StandingDTO parseStandingNode(JsonNode node) {
        JsonNode driver  = node.path("Driver");
        JsonNode constrs = node.path("Constructors");
        String constructorName = (constrs.isArray() && !constrs.isEmpty())
            ? constrs.get(0).path("name").asText("")
            : "";

        return StandingDTO.builder()
            .position(node.path("position").asInt())
            .driverId(driver.path("driverId").asText())
            .driverCode(driver.path("code").asText(""))
            .driverName(driver.path("givenName").asText("")
                      + " " + driver.path("familyName").asText(""))
            .constructorName(constructorName)
            .points(node.path("points").asDouble(0.0))
            .wins(node.path("wins").asInt(0))
            .build();
    }
}
