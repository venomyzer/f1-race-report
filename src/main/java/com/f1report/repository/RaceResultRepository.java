package com.f1report.repository;

import com.f1report.model.RaceResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RaceResultRepository – data access for individual driver race results.
 *
 * Uses JOIN FETCH in some queries to eagerly load related Driver data
 * in the same SQL query. Without JOIN FETCH, Hibernate would fire N+1
 * SELECT statements (one per result row) to load each Driver – slow!
 *
 * N+1 Problem explained:
 *   Without JOIN FETCH:  1 query for results + 20 queries (one per driver) = 21 queries
 *   With JOIN FETCH:     1 query with a SQL JOIN = 1 query total
 *
 * Real-world analogy: instead of 20 separate trips to the warehouse to pick
 * up each item, you drive one truck and load everything in one trip.
 */
@Repository
public interface RaceResultRepository extends JpaRepository<RaceResult, Long> {

    /**
     * Get all results for a race (identified by race PK), with driver data
     * loaded in a single query, sorted by finishing position.
     */
    @Query("""
        SELECT rr FROM RaceResult rr
        JOIN FETCH rr.driver
        WHERE rr.race.id = :raceId
        ORDER BY rr.finishingPosition ASC NULLS LAST
        """)
    List<RaceResult> findByRaceIdWithDriver(@Param("raceId") Long raceId);

    /**
     * Find a driver's result in a specific race.
     * Used to check if results are already persisted before re-importing.
     */
    @Query("""
        SELECT rr FROM RaceResult rr
        WHERE rr.race.id = :raceId AND rr.driver.driverId = :driverId
        """)
    Optional<RaceResult> findByRaceIdAndDriverId(
        @Param("raceId")   Long raceId,
        @Param("driverId") String driverId
    );

    /**
     * Get all results for a specific driver across a full season.
     * Used for driver performance charts across multiple races.
     */
    @Query("""
        SELECT rr FROM RaceResult rr
        JOIN FETCH rr.race
        WHERE rr.driver.driverId = :driverId
          AND rr.race.season = :season
        ORDER BY rr.race.round ASC
        """)
    List<RaceResult> findByDriverAndSeason(
        @Param("driverId") String driverId,
        @Param("season")   Integer season
    );

    /**
     * Check if results are already stored for a race.
     * Prevents duplicate imports from Ergast.
     */
    boolean existsByRaceId(Long raceId);
}
