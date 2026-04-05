package com.f1report.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * RaceResult – JPA entity for a single driver's finishing result in a race.
 *
 * This is the JOIN between a Race and a Driver.
 * Real-world analogy: like a report card. The student (Driver) and the term
 * (Race) are separate entities; the report card (RaceResult) connects them
 * and holds the grade (position, points, time).
 *
 * @ManyToOne: many RaceResults belong to one Race (and one Driver).
 * @JoinColumn: the FK column in race_results that points to the race's PK.
 *
 * Database table: race_results
 * Columns: id, race_id (FK), driver_id (FK), position, grid, points,
 *          laps, status, finish_time, fastest_lap_time, constructor_name, etc.
 */
@Entity
@Table(name = "race_results",
       uniqueConstraints = @UniqueConstraint(columnNames = {"race_id", "driver_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Foreign Keys / Relationships ─────────────────────────────────────────

    /**
     * Which race this result belongs to.
     * FetchType.LAZY = don't load full Race object unless explicitly accessed.
     * @JoinColumn = the FK column in race_results table pointing to races.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Race race;

    /**
     * Which driver this result belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Driver driver;

    // ── Result Data ──────────────────────────────────────────────────────────

    /** Final finishing position (1 = winner, null if DNF before classification) */
    @Column(name = "finishing_position")
    private Integer finishingPosition;

    /** Starting grid position */
    @Column(name = "grid_position")
    private Integer gridPosition;

    /** Championship points awarded (e.g., 25 for winner) */
    @Column(name = "points")
    private Double points;

    /** Number of laps completed */
    @Column(name = "laps_completed")
    private Integer lapsCompleted;

    /**
     * Finishing status:
     * "Finished" = completed all laps
     * "+1 Lap"   = finished but 1 lap behind the leader
     * "DNF"      = Did Not Finish (engine, crash, etc.)
     * "DSQ"      = Disqualified
     */
    @Column(length = 50)
    private String status;

    /** Finish time (e.g., "1:33:56.736" for winner, "+5.250" for others) */
    @Column(name = "finish_time", length = 50)
    private String finishTime;

    /** Fastest lap time set by this driver (e.g., "1:32.608") */
    @Column(name = "fastest_lap_time", length = 50)
    private String fastestLapTime;

    /** Fastest lap rank (1 = set the overall fastest lap) */
    @Column(name = "fastest_lap_rank")
    private Integer fastestLapRank;

    /** Average speed during fastest lap (kph) */
    @Column(name = "fastest_lap_avg_speed")
    private Double fastestLapAvgSpeed;

    /** Team/constructor name (e.g., "Red Bull") */
    @Column(name = "constructor_name", length = 100)
    private String constructorName;

    /** Car number during the race */
    @Column(name = "car_number", length = 5)
    private String carNumber;
}
