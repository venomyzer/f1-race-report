package com.f1report.service;

import com.f1report.dto.*;
import com.f1report.dto.RaceDataDTO.StandingDTO;
import com.f1report.model.*;
import com.f1report.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RaceDataService – orchestrates all race data operations.
 *
 * This is the "conductor" of the backend:
 *   1. Checks the DB for cached data first (fast path)
 *   2. If not found, delegates to ErgastService to fetch from the API
 *   3. Persists fetched data to PostgreSQL (so future calls hit the DB)
 *   4. Assembles the master RaceDataDTO from multiple data sources
 *   5. Enriches lap data with driver names/constructor info
 *
 * Real-world analogy: like a race team's data engineer. They coordinate
 * between the trackside timing feed (Ergast), the team's own database,
 * and the engineers who need the data – making sure everyone gets the
 * right information in the right format.
 *
 * @Transactional: all DB writes inside a method either ALL succeed or ALL
 * roll back. Like a bank transfer – either both accounts update, or neither.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RaceDataService {

    private final ErgastService         ergastService;
    private final RaceRepository        raceRepository;
    private final DriverRepository      driverRepository;
    private final RaceResultRepository  raceResultRepository;

    // ═════════════════════════════════════════════════════════════════════════
    // PUBLIC METHODS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Get all races for a season.
     * Strategy: try DB first, fall back to Ergast API, then persist.
     *
     * @param season The F1 championship year (e.g., 2024)
     */
    public List<RaceDTO> getRacesForSeason(int season) {
        long dbCount = raceRepository.countBySeason(season);

        if (dbCount > 0) {
            log.debug("Returning {} races for season {} from DB", dbCount, season);
            return raceRepository.findBySeasonOrderByRoundAsc(season)
                .stream()
                .map(this::raceEntityToDTO)
                .collect(Collectors.toList());
        }

        // Not in DB → fetch from Ergast API, then persist
        log.info("Season {} not in DB – fetching from Ergast", season);
        List<RaceDTO> races = ergastService.fetchRacesBySeason(season);
        persistRaces(races);
        return races;
    }

    /**
     * Build the full RaceDataDTO for a specific race.
     *
     * This method:
     *   1. Fetches race metadata (season, round, circuit)
     *   2. Fetches all driver results (20 rows)
     *   3. Fetches lap-by-lap position data (for line chart)
     *   4. Fetches championship standings (for bar chart)
     *   5. Enriches lap data with driver code→name mappings
     *   6. Assembles everything into one RaceDataDTO
     *
     * @param season The season year
     * @param round  The round number within the season
     */
    public RaceDataDTO getRaceData(int season, int round) {
        log.info("Building RaceDataDTO for S{} R{}", season, round);

        // ── Step 1: Fetch results from Ergast (or DB cache) ──────────────────
        List<DriverResultDTO> results = getOrFetchResults(season, round);

        if (results.isEmpty()) {
            throw new RuntimeException(
                String.format("No race data found for Season %d Round %d. " +
                              "The race may not have taken place yet.", season, round));
        }

        // ── Step 2: Fetch race metadata ───────────────────────────────────────
        RaceDTO raceMeta = getRaceMetadata(season, round);

        // ── Step 3: Fetch lap data ────────────────────────────────────────────
        LapDataDTO lapData = ergastService.fetchLapData(season, round);

        // Enrich lap data with driver names (lap data has driverId, we want code+name)
        enrichLapData(lapData, results);

        // ── Step 4: Fetch championship standings ──────────────────────────────
        List<StandingDTO> standings = ergastService.fetchDriverStandings(season, round);

        // ── Step 5: Compute summary stats ─────────────────────────────────────
        DriverResultDTO winner = results.stream()
            .filter(r -> r.getFinishingPosition() != null && r.getFinishingPosition() == 1)
            .findFirst()
            .orElse(results.get(0));

        DriverResultDTO fastestLapHolder = results.stream()
            .filter(DriverResultDTO::hasFastestLap)
            .findFirst()
            .orElse(null);

        long classified  = results.stream().filter(DriverResultDTO::isClassifiedFinish).count();
        long retirements = results.size() - classified;

        int totalLaps = lapData.isLapDataAvailable()
            ? lapData.getTotalLaps()
            : winner.getLapsCompleted() != null ? winner.getLapsCompleted() : 0;

        // ── Step 6: Extract podium (top 3) ────────────────────────────────────
        List<DriverResultDTO> podium = results.stream()
            .filter(r -> r.getFinishingPosition() != null && r.getFinishingPosition() <= 3)
            .sorted(Comparator.comparing(DriverResultDTO::getFinishingPosition))
            .collect(Collectors.toList());

        // ── Step 7: Assemble master DTO ───────────────────────────────────────
        return RaceDataDTO.builder()
            .season(season)
            .round(round)
            .raceName(raceMeta != null ? raceMeta.getRaceName() : "Unknown Race")
            .circuitName(raceMeta != null ? raceMeta.getCircuitName() : "")
            .country(raceMeta != null ? raceMeta.getCountry() : "")
            .locality(raceMeta != null ? raceMeta.getLocality() : "")
            .raceDate(raceMeta != null ? raceMeta.getRaceDate() : "")
            .results(results)
            .podium(podium)
            .lapData(lapData)
            .winnerName(winner.getDriverName())
            .winnerConstructor(winner.getConstructorName())
            .winnerTime(winner.getFinishTime())
            .fastestLapHolder(fastestLapHolder != null ? fastestLapHolder.getDriverName() : null)
            .fastestLapTime(fastestLapHolder != null ? fastestLapHolder.getFastestLapTime() : null)
            .totalLaps(totalLaps)
            .classifiedFinishers((int) classified)
            .retirements((int) retirements)
            .driverStandings(standings)
            .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Get race results from DB if persisted, otherwise fetch from Ergast and persist.
     * This is the "DB-first, API-fallback" pattern.
     */
    private List<DriverResultDTO> getOrFetchResults(int season, int round) {
        // Check if race exists in DB
        Optional<Race> raceOpt = raceRepository.findBySeasonAndRound(season, round);

        if (raceOpt.isPresent() && raceResultRepository.existsByRaceId(raceOpt.get().getId())) {
            log.debug("Returning results for S{} R{} from DB", season, round);
            return raceResultRepository.findByRaceIdWithDriver(raceOpt.get().getId())
                .stream()
                .map(this::resultEntityToDTO)
                .collect(Collectors.toList());
        }

        // Fetch from Ergast
        log.info("Results for S{} R{} not in DB – fetching from Ergast", season, round);
        List<DriverResultDTO> results = ergastService.fetchRaceResults(season, round);

        // Persist to DB asynchronously in background
        if (!results.isEmpty()) {
            persistResults(season, round, results);
        }

        return results;
    }

    /**
     * Get race metadata for the given season/round.
     * First tries the DB, then fetches from the Ergast season race list.
     */
    private RaceDTO getRaceMetadata(int season, int round) {
        return raceRepository.findBySeasonAndRound(season, round)
            .map(this::raceEntityToDTO)
            .orElseGet(() -> {
                // Fetch the full season race list and find our round
                return ergastService.fetchRacesBySeason(season)
                    .stream()
                    .filter(r -> r.getRound().equals(round))
                    .findFirst()
                    .orElse(null);
            });
    }

    /**
     * Enrich LapDataDTO with driver name and constructor mappings.
     *
     * The Ergast lap API uses driverId (e.g., "verstappen") as keys.
     * The frontend wants short codes (e.g., "VER") and full names.
     * We build those mappings from the results we already fetched.
     *
     * Before enrichment: lapData.driverCodes = ["verstappen", "hamilton", ...]
     * After enrichment:  lapData.driverNames = { "verstappen": "Max Verstappen", ... }
     *                    lapData.driverConstructors = { "verstappen": "Red Bull", ... }
     */
    private void enrichLapData(LapDataDTO lapData, List<DriverResultDTO> results) {
        if (!lapData.isLapDataAvailable()) return;

        // Build lookup maps from results
        Map<String, String> idToName        = new HashMap<>();
        Map<String, String> idToConstructor = new HashMap<>();
        Map<String, String> idToCode        = new HashMap<>();

        results.forEach(r -> {
            idToName.put(r.getDriverId(), r.getDriverName());
            idToConstructor.put(r.getDriverId(), r.getConstructorName());
            idToCode.put(r.getDriverId(), r.getDriverCode());
        });

        lapData.setDriverNames(idToName);
        lapData.setDriverConstructors(idToConstructor);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PERSISTENCE METHODS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Persist a list of races from Ergast to our PostgreSQL DB.
     * @Transactional ensures all inserts succeed or all roll back.
     */
    @Transactional
    protected void persistRaces(List<RaceDTO> races) {
        races.forEach(dto -> {
            if (!raceRepository.existsBySeasonAndRound(dto.getSeason(), dto.getRound())) {
                Race race = Race.builder()
                    .season(dto.getSeason())
                    .round(dto.getRound())
                    .raceName(dto.getRaceName())
                    .circuitName(dto.getCircuitName())
                    .country(dto.getCountry())
                    .locality(dto.getLocality())
                    .raceDate(dto.getRaceDate() != null && !dto.getRaceDate().isEmpty()
                        ? LocalDate.parse(dto.getRaceDate()) : null)
                    .raceTime(dto.getRaceTime())
                    .build();
                raceRepository.save(race);
            }
        });
        log.info("Persisted {} races to DB", races.size());
    }

    /**
     * Persist race results and driver records to the DB.
     * For each result:
     *   1. Find or create the Driver record (upsert pattern)
     *   2. Find or create the Race record
     *   3. Create the RaceResult linking them
     */
    @Transactional
    protected void persistResults(int season, int round, List<DriverResultDTO> results) {
        try {
            // Ensure Race record exists
            Race race = raceRepository.findBySeasonAndRound(season, round)
                .orElseGet(() -> raceRepository.save(Race.builder()
                    .season(season)
                    .round(round)
                    .raceName("Season " + season + " Round " + round)
                    .build()));

            results.forEach(dto -> {
                // Upsert Driver
                Driver driver = driverRepository.findByDriverId(dto.getDriverId())
                    .orElseGet(() -> driverRepository.save(Driver.builder()
                        .driverId(dto.getDriverId())
                        .givenName(dto.getDriverName().contains(" ")
                            ? dto.getDriverName().split(" ")[0] : dto.getDriverName())
                        .familyName(dto.getDriverName().contains(" ")
                            ? dto.getDriverName().substring(dto.getDriverName().indexOf(" ") + 1)
                            : "")
                        .code(dto.getDriverCode())
                        .nationality(dto.getNationality())
                        .permanentNumber(dto.getCarNumber())
                        .build()));

                // Save RaceResult (only if not already persisted)
                boolean exists = raceResultRepository
                    .findByRaceIdAndDriverId(race.getId(), dto.getDriverId())
                    .isPresent();

                if (!exists) {
                    raceResultRepository.save(RaceResult.builder()
                        .race(race)
                        .driver(driver)
                        .finishingPosition(dto.getFinishingPosition())
                        .gridPosition(dto.getGridPosition())
                        .points(dto.getPoints())
                        .lapsCompleted(dto.getLapsCompleted())
                        .status(dto.getStatus())
                        .finishTime(dto.getFinishTime())
                        .fastestLapTime(dto.getFastestLapTime())
                        .fastestLapRank(dto.getFastestLapRank())
                        .fastestLapAvgSpeed(dto.getFastestLapAvgSpeed())
                        .constructorName(dto.getConstructorName())
                        .carNumber(dto.getCarNumber())
                        .build());
                }
            });
            log.info("Persisted {} results for S{} R{}", results.size(), season, round);
        } catch (Exception e) {
            log.error("Failed to persist results for S{} R{} – continuing without DB save", season, round, e);
            // Non-fatal: the data was already returned to the user; DB write failure is acceptable
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ENTITY → DTO MAPPERS
    // ═════════════════════════════════════════════════════════════════════════

    private RaceDTO raceEntityToDTO(Race race) {
        return RaceDTO.builder()
            .season(race.getSeason())
            .round(race.getRound())
            .raceName(race.getRaceName())
            .circuitName(race.getCircuitName())
            .country(race.getCountry())
            .locality(race.getLocality())
            .raceDate(race.getRaceDate() != null ? race.getRaceDate().toString() : "")
            .raceTime(race.getRaceTime())
            .build();
    }

    private DriverResultDTO resultEntityToDTO(RaceResult rr) {
        Driver d = rr.getDriver();
        return DriverResultDTO.builder()
            .driverId(d.getDriverId())
            .driverCode(d.getCode())
            .driverName(d.getFullName())
            .nationality(d.getNationality())
            .constructorName(rr.getConstructorName())
            .carNumber(rr.getCarNumber())
            .finishingPosition(rr.getFinishingPosition())
            .gridPosition(rr.getGridPosition())
            .points(rr.getPoints())
            .lapsCompleted(rr.getLapsCompleted())
            .status(rr.getStatus())
            .finishTime(rr.getFinishTime())
            .fastestLapTime(rr.getFastestLapTime())
            .fastestLapRank(rr.getFastestLapRank())
            .fastestLapAvgSpeed(rr.getFastestLapAvgSpeed())
            .build();
    }
}
