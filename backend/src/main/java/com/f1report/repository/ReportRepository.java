package com.f1report.repository;

import com.f1report.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ReportRepository – data access for AI-generated race reports.
 *
 * The key design decision here is the "generate once, store forever" pattern:
 *   1. Before calling the expensive Groq API, check if a report exists here.
 *   2. If yes → return it instantly from the DB (milliseconds).
 *   3. If no  → call Groq, generate, store, return.
 *
 * Real-world analogy: like a legal department that keeps a copy of every
 * contract ever drafted. When a client asks for the same contract again,
 * you hand them the filed copy instead of drafting from scratch.
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * Find a previously generated report for a specific race.
     * Returns the most recently generated report if multiple exist
     * (in case someone regenerated with a different model).
     */
    @Query("""
        SELECT r FROM Report r
        WHERE r.season = :season AND r.round = :round
        ORDER BY r.createdAt DESC
        LIMIT 1
        """)
    Optional<Report> findLatestBySeasonAndRound(
        @Param("season") Integer season,
        @Param("round")  Integer round
    );

    /**
     * Get all reports for a season, ordered by round.
     * Used to display a "report history" list in the frontend.
     */
    List<Report> findBySeasonOrderByRoundAsc(Integer season);

    /**
     * Check if any report exists for this race (fast existence check).
     * Used before deciding whether to call the Groq API.
     */
    boolean existsBySeasonAndRound(Integer season, Integer round);

    /**
     * Get the 10 most recently generated reports across all seasons.
     * Used for a "Recent Reports" widget on the dashboard.
     */
    @Query("""
        SELECT r FROM Report r
        ORDER BY r.createdAt DESC
        LIMIT 10
        """)
    List<Report> findTop10RecentReports();
}
