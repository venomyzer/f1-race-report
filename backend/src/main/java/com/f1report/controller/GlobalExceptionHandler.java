package com.f1report.controller;

import com.f1report.dto.ApiResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler – catches all unhandled exceptions thrown from any
 * controller and returns a structured, user-friendly JSON error response.
 *
 * Without this: Spring would return a raw HTML error page (the "white label"
 * error page), which the React frontend cannot parse.
 *
 * With this: every error becomes a consistent ApiResponseDTO JSON response.
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 *   → this class intercepts exceptions from ALL @RestController classes.
 *   → method return values are automatically serialised to JSON.
 *
 * Real-world analogy: like a customer service representative who intercepts
 * all complaints from any department. Instead of the customer getting a raw
 * internal error message ("NullPointerException at line 42"), they get a
 * polite, structured response: "We're sorry, your request couldn't be processed."
 *
 * Exception handler priority: Spring picks the MOST SPECIFIC handler.
 * If you throw a MethodArgumentNotValidException, it uses that handler.
 * If no specific handler matches, it falls back to handleGenericException().
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle @Valid validation failures on @RequestBody parameters.
     *
     * Triggered when: a POST /api/generate-report body fails validation
     * (e.g., season=null, round=-1, missing required field).
     *
     * Returns HTTP 400 Bad Request with field-level error map:
     * {
     *   "success": false,
     *   "message": "Validation failed",
     *   "data": { "season": "Season is required", "round": "Round must be at least 1" },
     *   "statusCode": 400
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName    = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity.badRequest()
            .body(ApiResponseDTO.<Map<String, String>>builder()
                .success(false)
                .message("Validation failed: please check the request body")
                .data(fieldErrors)
                .statusCode(400)
                .build());
    }

    /**
     * Handle missing required @RequestParam parameters.
     * Triggered when: GET /api/races is called without ?season=YYYY
     *
     * Returns HTTP 400:
     * { "success": false, "message": "Required parameter 'season' is missing", ... }
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleMissingParam(
            MissingServletRequestParameterException ex) {
        String message = "Required parameter '" + ex.getParameterName() + "' is missing";
        log.warn("Missing request parameter: {}", ex.getParameterName());
        return ResponseEntity.badRequest()
            .body(ApiResponseDTO.error(message, 400));
    }

    /**
     * Handle wrong type for @RequestParam/@PathVariable.
     * Triggered when: GET /api/races?season=abc (non-integer season)
     *
     * Returns HTTP 400:
     * { "success": false, "message": "Invalid value 'abc' for parameter 'season'" }
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
            ex.getValue(), ex.getName(),
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        log.warn("Type mismatch: {}", message);
        return ResponseEntity.badRequest()
            .body(ApiResponseDTO.error(message, 400));
    }

    /**
     * Handle all uncaught RuntimeExceptions from the service layer.
     *
     * This is the catch-all for anything not handled more specifically above.
     * We log the full stack trace (for debugging) but return a clean message
     * to the client (avoid exposing internal stack traces to end users).
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("Unhandled RuntimeException: {}", ex.getMessage(), ex);

        // Determine appropriate HTTP status based on message content
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("not found"))         status = HttpStatus.NOT_FOUND;
            else if (ex.getMessage().contains("unavailable"))  status = HttpStatus.SERVICE_UNAVAILABLE;
            else if (ex.getMessage().contains("Invalid"))      status = HttpStatus.BAD_REQUEST;
        }

        return ResponseEntity.status(status)
            .body(ApiResponseDTO.error(
                ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred",
                status.value()));
    }

    /**
     * Catch-all for any Exception not matched above.
     * Logs with ERROR level and returns a generic 500 message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
            .body(ApiResponseDTO.error(
                "An internal server error occurred. Please try again later.", 500));
    }
}
