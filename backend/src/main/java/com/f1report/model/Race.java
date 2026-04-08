package com.f1report.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Race – JPA entity representing a single Formula 1 Grand Prix event.
 *
 * Maps to the "races" table. Each Race belongs to a season (year) and
 * has a round number (e.g., Round 1 = Bahrain GP, Round 5 = Miami GP).
 *
 * @OneToMany relationship: one Race → many RaceResults (one race has many
 * driver finishing positions). This is like one Order → many OrderItems.
 *
 * FetchType.LAZY means results are NOT loaded from the DB automatically
 * when you fetch a Race. They're loaded only when you explicitly access
 * race.getResults(). This avoids loading thousands of result rows unintentionally.
 */
@Entity
@Table(name = "races",
       uniqueConstraints = @UniqueConstraint(columnNames = {"season", "round"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Race {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The championship year (e.g., 2024) */
    @Column(nullable = false)
    private Integer season;

    /** Race number within the season (1 = first race of the year) */
    @Column(nullable = false)
    private Integer round;

    /** Official Grand Prix name (e.g., "Bahrain Grand Prix") */
    @Column(name = "race_name", nullable = false, length = 200)
    private String raceName;

    // ── Circuit details ──────────────────────────────────────────────────────

    @Column(name = "circuit_id", length = 100)
    private String circuitId;

    @Column(name = "circuit_name", length = 200)
    private String circuitName;

    /** Country where the circuit is located (e.g., "Bahrain") */
    @Column(length = 100)
    private String country;

    /** City/locality (e.g., "Sakhir") */
    @Column(length = 100)
    private String locality;

    // ── Scheduling ───────────────────────────────────────────────────────────

    @Column(name = "race_date")
    private LocalDate raceDate;

    @Column(name = "race_time", length = 20)
    private String raceTime;

    // ── Results relationship ─────────────────────────────────────────────────
    /**
     * One Race has many RaceResult rows.
     * mappedBy = "race" points to the "race" field inside RaceResult.
     * cascade = ALL: if a Race is deleted, all its results are also deleted.
     */
    @OneToMany(mappedBy = "race", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude   // avoid infinite loop in toString() (Race→Results→Race)
    @EqualsAndHashCode.Exclude
    private List<RaceResult> results;
}
