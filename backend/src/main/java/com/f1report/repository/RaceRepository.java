package com.f1report.repository;

import com.f1report.model.Race;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RaceRepository – data access layer for Race entities.
 *
 * Custom @Query examples:
 *   • @Query uses JPQL (Java Persistence Query Language) – similar to SQL
 *     but operates on entity class names/fields, not table/column names.
 *   • "SELECT r FROM Race r WHERE r.season = :season" → Hibernate translates
 *     this to "SELECT * FROM races WHERE season = ?" automatically.
 *
 * Why JPQL over SQL?
 *   • DB-agnostic: works on PostgreSQL, MySQL, H2 without changes.
 *   • Type-safe: references Java field names, so a rename refactor is caught.
 *   • In production, complex queries use @Query(nativeQuery=true) for
 *     PostgreSQL-specific features (window functions, CTEs, JSONB, etc.)
 */
@Repository
public interface RaceRepository extends JpaRepository<Race, Long> {

    /**
     * Get all races for a given season, ordered by round number.
     * Real-world use: populate the "Select Race" dropdown for a chosen year.
     *
     * Generated SQL: SELECT * FROM races WHERE season = ? ORDER BY round ASC
     */
    List<Race> findBySeasonOrderByRoundAsc(Integer season);

    /**
     * Find a specific race by season + round (the natural compound key).
     * Used for de-duplication when importing from Ergast.
     *
     * Generated SQL: SELECT * FROM races WHERE season = ? AND round = ?
     */
    Optional<Race> findBySeasonAndRound(Integer season, Integer round);

    /**
     * Get a distinct sorted list of all seasons we have races stored for.
     * @Query with JPQL DISTINCT to avoid duplicates.
     *
     * Generated SQL: SELECT DISTINCT season FROM races ORDER BY season DESC
     */
    @Query("SELECT DISTINCT r.season FROM Race r ORDER BY r.season DESC")
    List<Integer> findDistinctSeasons();

    /**
     * Check if we already have race data cached for a given season + round.
     * Used to decide whether to call Ergast API or return from DB.
     */
    boolean existsBySeasonAndRound(Integer season, Integer round);

    /**
     * Count how many races we have stored for a season.
     * Useful for verifying a full season calendar is loaded.
     */
    @Query("SELECT COUNT(r) FROM Race r WHERE r.season = :season")
    long countBySeason(@Param("season") Integer season);
}
