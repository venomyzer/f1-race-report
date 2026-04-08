package com.f1report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SeasonDTO – represents a single F1 season in API responses.
 *
 * DTOs (Data Transfer Objects) are "view models" – they shape data specifically
 * for the API contract between backend and frontend.
 *
 * Why NOT return the JPA entity directly?
 *   1. Entities may have circular references (Race → Results → Race) that
 *      cause infinite loops during JSON serialisation.
 *   2. Entities often contain fields the client doesn't need (createdAt, etc.).
 *   3. DTOs give you a stable API contract: you can change the DB schema
 *      without breaking the frontend as long as the DTO stays the same.
 *
 * Real-world analogy: a DTO is like a press release. The full internal report
 * (entity) has sensitive internal details. The press release (DTO) shows only
 * what the public (frontend) needs to see.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonDTO {
    private Integer season;   // e.g., 2024
    private String  url;      // Wikipedia URL for the season
}
