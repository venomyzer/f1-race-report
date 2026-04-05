// ─────────────────────────────────────────────────────────────────────────────
// FILE: dto/ApiResponseDTO.java
// Generic API response wrapper – every endpoint returns this shape.
// Real-world analogy: like a standardised parcel box. No matter what's inside
// (seasons list, race results, AI report), the outer packaging is always the
// same, so the client (React) always knows how to unwrap it.
// ─────────────────────────────────────────────────────────────────────────────
package com.f1report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDTO<T> {
    private boolean success;
    private String  message;
    private T       data;
    private int     statusCode;

    // ── Static factory helpers ───────────────────────────────────────────────

    /** Build a successful response wrapping any data payload */
    public static <T> ApiResponseDTO<T> ok(T data) {
        return ApiResponseDTO.<T>builder()
            .success(true)
            .message("OK")
            .data(data)
            .statusCode(200)
            .build();
    }

    /** Build a successful response with a custom message */
    public static <T> ApiResponseDTO<T> ok(T data, String message) {
        return ApiResponseDTO.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .statusCode(200)
            .build();
    }

    /** Build an error response */
    public static <T> ApiResponseDTO<T> error(String message, int statusCode) {
        return ApiResponseDTO.<T>builder()
            .success(false)
            .message(message)
            .statusCode(statusCode)
            .build();
    }
}
