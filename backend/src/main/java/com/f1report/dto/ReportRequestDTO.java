package com.f1report.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ReportRequestDTO – the request body for POST /api/generate-report.
 *
 * The frontend sends this JSON body:
 * {
 *   "season": 2024,
 *   "round": 5,
 *   "forceRegenerate": false
 * }
 *
 * @NotNull, @Min, @Max are Bean Validation annotations.
 * When the controller uses @Valid on the @RequestBody parameter, Spring
 * automatically validates these constraints BEFORE the method runs.
 * If validation fails, Spring returns a 400 Bad Request with field errors –
 * no manual if/else validation code needed.
 *
 * Real-world analogy: like an online order form where the website won't
 * let you submit if you haven't filled in required fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestDTO {

    @NotNull(message = "Season is required")
    @Min(value = 1950, message = "Season must be 1950 or later")
    @Max(value = 2099, message = "Season must be a valid year")
    private Integer season;

    @NotNull(message = "Round is required")
    @Min(value = 1, message = "Round must be at least 1")
    @Max(value = 30, message = "Round cannot exceed 30")
    private Integer round;

    /**
     * If true, regenerate even if a report exists in the DB.
     * Useful if the user wants a fresh AI perspective or the previous
     * report was unsatisfactory.
     * Defaults to false (use cached report if available).
     */
    @Builder.Default
    private boolean forceRegenerate = false;
}


// ─────────────────────────────────────────────────────────────────────────────
// FILE: dto/ReportResponseDTO.java
// ─────────────────────────────────────────────────────────────────────────────


// Note: In a real multi-file project each class lives in its own file.
// Placing both here for organisation; split them before compiling.

// package com.f1report.dto;  ← already declared above, same package
