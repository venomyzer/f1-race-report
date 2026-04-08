package com.f1report.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Report – JPA entity for storing AI-generated race reports.
 *
 * Every time a user generates a report for a specific race, we:
 *   1. Call the Groq AI API (expensive, slow)
 *   2. Store the result here (cheap, instant on subsequent requests)
 *   3. Return from DB on future requests for the same race
 *
 * Real-world analogy: like a newspaper archive. The journalist (Groq AI)
 * writes the article once. After that, readers get the stored copy from
 * the archive – no one has to interview sources again.
 *
 * @CreationTimestamp: Hibernate automatically sets createdAt to NOW()
 * when the entity is first persisted – no manual date setting needed.
 */
@Entity
@Table(name = "reports",
       indexes = {
           // Index on (season, round) because we frequently look up
           // "does a report exist for this race?" before calling Groq.
           @Index(name = "idx_reports_season_round", columnList = "season, round")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The F1 season year (e.g., 2024) */
    @Column(nullable = false)
    private Integer season;

    /** The race round number within the season */
    @Column(nullable = false)
    private Integer round;

    /** Official race name (e.g., "Monaco Grand Prix") – for display */
    @Column(name = "race_name", length = 200)
    private String raceName;

    /**
     * The full AI-generated text report.
     * @Lob (Large Object) stores this in a TEXT column in PostgreSQL,
     * which can hold up to 1 GB. A typical AI report is ~2000 chars.
     */
    @Lob
    @Column(name = "report_content", nullable = false, columnDefinition = "TEXT")
    private String reportContent;

    /** The AI model version used to generate this report (for auditability) */
    @Column(name = "model_used", length = 100)
    private String modelUsed;

    /** The prompt tokens used (for monitoring Groq API usage) */
    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    /** The completion tokens generated (for monitoring Groq API usage) */
    @Column(name = "completion_tokens")
    private Integer completionTokens;

    /**
     * Timestamp when this report was created.
     * @CreationTimestamp = Hibernate sets this automatically on INSERT.
     * updatable = false = prevents Hibernate from overwriting it on UPDATE.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Winner's name (denormalized for quick display without joining) */
    @Column(name = "winner_name", length = 200)
    private String winnerName;

    /** Winner's constructor/team */
    @Column(name = "winner_constructor", length = 100)
    private String winnerConstructor;
}
