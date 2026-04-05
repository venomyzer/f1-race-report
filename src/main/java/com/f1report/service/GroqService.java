package com.f1report.service;

import com.f1report.dto.DriverResultDTO;
import com.f1report.dto.RaceDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GroqService – integrates with the Groq AI API for race report generation.
 *
 * Groq provides fast LLM inference using the LPU (Language Processing Unit).
 * It exposes an OpenAI-compatible API, meaning the request/response schema
 * is identical to OpenAI's /v1/chat/completions endpoint.
 *
 * Our approach:
 *   1. Take the structured RaceDataDTO (20 driver results, lap stats, etc.)
 *   2. Format it into a rich, detailed prompt (the "journalist briefing")
 *   3. POST it to Groq with system instructions defining the AI's persona
 *   4. Parse the response and extract the generated text + token usage
 *
 * Real-world analogy: GroqService is the editor who hands a briefing folder
 * to a journalist (the LLM). The folder contains all the raw race stats.
 * The journalist reads it and writes the magazine-quality race report.
 *
 * Endpoint: POST https://api.groq.com/openai/v1/chat/completions
 * Auth:     Authorization: Bearer {GROQ_API_KEY}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroqService {

    @Qualifier("groqRestTemplate")
    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.base-url}")
    private String baseUrl;

    @Value("${groq.api.model}")
    private String model;

    @Value("${groq.api.max-tokens}")
    private int maxTokens;

    @Value("${groq.api.temperature}")
    private double temperature;

    // ── Response holder (inner record) ───────────────────────────────────────

    /**
     * GroqResponse – a simple holder for the AI output and token metadata.
     * Using a Java record (Java 16+) for immutable data carriers.
     *
     * @param reportText        The generated journalist-style race report text
     * @param promptTokens      Tokens used in the prompt (input)
     * @param completionTokens  Tokens used in the completion (output)
     * @param modelUsed         The model that generated the response
     */
    public record GroqResponse(
        String reportText,
        int    promptTokens,
        int    completionTokens,
        String modelUsed
    ) {}

    // ═════════════════════════════════════════════════════════════════════════
    // MAIN PUBLIC METHOD
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Generate an AI race report for the given race data.
     *
     * @param raceData The full race data DTO (results, standings, etc.)
     * @return GroqResponse containing the report text and token usage
     */
    public GroqResponse generateRaceReport(RaceDataDTO raceData) {
        log.info("Generating AI race report for {} S{} R{} via Groq",
            raceData.getRaceName(), raceData.getSeason(), raceData.getRound());

        String prompt  = buildPrompt(raceData);
        String payload = buildRequestPayload(prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);    // Authorization: Bearer {key}

        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        String endpoint = baseUrl + "/chat/completions";

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                endpoint, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Groq API returned status: " + response.getStatusCode());
            }

            return parseGroqResponse(response.getBody());

        } catch (Exception e) {
            log.error("Groq API call failed for S{} R{}", raceData.getSeason(), raceData.getRound(), e);
            throw new RuntimeException("AI report generation failed: " + e.getMessage(), e);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PROMPT ENGINEERING
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Build the detailed user prompt from race data.
     *
     * Prompt engineering principles applied here:
     *   1. ROLE DEFINITION  – tell the model who it is ("F1 journalist for Autosport")
     *   2. STRUCTURED INPUT – provide data in a clear, labelled format
     *   3. SPECIFIC TASKS   – enumerate exactly what to include in the report
     *   4. FORMAT GUIDANCE  – specify length, tone, section headings
     *   5. CONSTRAINTS      – "do not invent facts not present in the data"
     *
     * Real-world analogy: the prompt is like a detailed commissioning brief
     * a magazine editor hands to a freelance journalist before the race.
     * The journalist (LLM) follows the brief to produce a publishable piece.
     */
    private String buildPrompt(RaceDataDTO raceData) {
        StringBuilder sb = new StringBuilder();

        // ── Race Header ──────────────────────────────────────────────────────
        sb.append("=== RACE DATA BRIEFING ===\n\n");
        sb.append(String.format("Race: %s\n", raceData.getRaceName()));
        sb.append(String.format("Season: %d | Round: %d\n", raceData.getSeason(), raceData.getRound()));
        sb.append(String.format("Circuit: %s, %s, %s\n",
            raceData.getCircuitName(), raceData.getLocality(), raceData.getCountry()));
        sb.append(String.format("Date: %s\n\n", raceData.getRaceDate()));

        // ── Race Summary Stats ────────────────────────────────────────────────
        sb.append("=== RACE SUMMARY ===\n");
        sb.append(String.format("Winner: %s (%s)\n", raceData.getWinnerName(), raceData.getWinnerConstructor()));
        sb.append(String.format("Winning Time: %s\n", raceData.getWinnerTime()));
        sb.append(String.format("Total Laps: %d\n", raceData.getTotalLaps()));
        sb.append(String.format("Classified Finishers: %d | Retirements: %d\n",
            raceData.getClassifiedFinishers(), raceData.getRetirements()));
        if (raceData.getFastestLapHolder() != null) {
            sb.append(String.format("Fastest Lap: %s – %s\n\n",
                raceData.getFastestLapHolder(), raceData.getFastestLapTime()));
        } else {
            sb.append("\n");
        }

        // ── Podium ───────────────────────────────────────────────────────────
        sb.append("=== PODIUM ===\n");
        if (raceData.getPodium() != null) {
            raceData.getPodium().forEach(p -> sb.append(String.format(
                "P%d: %s (%s) | Points: %.0f | Time: %s\n",
                p.getFinishingPosition(), p.getDriverName(),
                p.getConstructorName(), p.getPoints(),
                p.getFinishTime() != null ? p.getFinishTime() : "N/A"
            )));
        }
        sb.append("\n");

        // ── Full Results (top 10 in detail, rest summarised) ─────────────────
        sb.append("=== RACE RESULTS ===\n");
        if (raceData.getResults() != null) {
            raceData.getResults().forEach(r -> {
                String pos = r.getFinishingPosition() != null
                    ? "P" + r.getFinishingPosition() : "DNF";
                sb.append(String.format(
                    "%s | %s (%s) | Grid: P%d | Pts: %.0f | Status: %s%s\n",
                    pos, r.getDriverName(), r.getConstructorName(),
                    r.getGridPosition() != null ? r.getGridPosition() : 0,
                    r.getPoints() != null ? r.getPoints() : 0.0,
                    r.getStatus(),
                    r.hasFastestLap() ? " ⚡FASTEST LAP" : ""
                ));
            });
        }
        sb.append("\n");

        // ── Championship Standings (top 10) ───────────────────────────────────
        if (raceData.getDriverStandings() != null && !raceData.getDriverStandings().isEmpty()) {
            sb.append("=== CHAMPIONSHIP STANDINGS AFTER THIS RACE (Top 10) ===\n");
            raceData.getDriverStandings().stream()
                .limit(10)
                .forEach(s -> sb.append(String.format(
                    "P%d: %s (%s) – %.0f pts\n",
                    s.getPosition(), s.getDriverName(), s.getConstructorName(), s.getPoints()
                )));
            sb.append("\n");
        }

        // ── Notable grid position changes ─────────────────────────────────────
        sb.append("=== NOTABLE POSITION CHANGES ===\n");
        if (raceData.getResults() != null) {
            raceData.getResults().stream()
                .filter(r -> r.getFinishingPosition() != null && r.getGridPosition() != null)
                .filter(r -> Math.abs(r.getFinishingPosition() - r.getGridPosition()) >= 3)
                .forEach(r -> {
                    int change = r.getGridPosition() - r.getFinishingPosition();
                    sb.append(String.format(
                        "%s started P%d, finished P%d (%s%d places)\n",
                        r.getDriverName(), r.getGridPosition(), r.getFinishingPosition(),
                        change > 0 ? "gained " : "lost ", Math.abs(change)
                    ));
                });
        }
        sb.append("\n");

        // ── Writing Instructions ───────────────────────────────────────────────
        sb.append("=== YOUR TASK ===\n");
        sb.append("""
            Write a professional Formula 1 race report as if you are a senior journalist
            for Autosport magazine. Your report should be engaging, analytical, and capture
            the drama of the race. Structure it as follows:

            **[Race Name] Race Report**

            **Race Overview** (2-3 sentences: result and overall narrative)

            **The Victory** (detail the winner's race – strategy, pace, key moments)

            **The Battle for the Podium** (describe P2 and P3 finishers)

            **Key Battles & Overtakes** (highlight the most dramatic position changes)

            **Strategy & Pit Stops** (infer strategy from grid vs finish positions)

            **The Retirements** (mention any DNFs and their impact)

            **Championship Implications** (how this result affects the title fight)

            **Driver of the Day** (pick one driver who stood out, with reasoning)

            **Verdict** (1-2 sentences: what this race means for the season)

            RULES:
            - Use only data provided above. Do NOT invent lap times, strategies, or incidents.
            - Write in present-perfect journalistic style ("Verstappen took the lead", not "he did")
            - Use driver surnames after first mention (e.g., "Max Verstappen" then "Verstappen")
            - Target ~600-800 words total
            - Use markdown bold for section headings (**Heading**)
            """);

        return sb.toString();
    }

    /**
     * Build the JSON request body for the Groq API.
     *
     * Groq uses the OpenAI chat completion format:
     * {
     *   "model": "llama-3.3-70b-versatile",
     *   "messages": [
     *     { "role": "system", "content": "You are..." },
     *     { "role": "user",   "content": "Write a report for..." }
     *   ],
     *   "max_tokens": 2048,
     *   "temperature": 0.75
     * }
     *
     * The "system" message defines the AI's persona and global constraints.
     * The "user" message is the specific request with race data.
     */
    private String buildRequestPayload(String userPrompt) {
        try {
            ObjectNode root    = objectMapper.createObjectNode();
            ArrayNode  messages = objectMapper.createArrayNode();

            // System message: sets the AI's role and tone
            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content",
                "You are an expert Formula 1 journalist with 20 years of experience " +
                "writing for publications like Autosport, Motorsport Magazine, and The Race. " +
                "You write with authority, passion, and technical precision. " +
                "You understand F1 strategy, aerodynamics, and the personalities of every driver. " +
                "You always write factual, data-driven reports without embellishing or inventing details. " +
                "Your prose is vivid, engaging, and accessible to both hardcore fans and casual readers."
            );
            messages.add(systemMsg);

            // User message: the race data briefing + writing instructions
            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);
            messages.add(userMsg);

            root.put("model", model);
            root.set("messages", messages);
            root.put("max_tokens", maxTokens);
            root.put("temperature", temperature);
            root.put("top_p", 0.9);           // Nucleus sampling – good variety without randomness
            root.put("stream", false);         // We want the full response at once, not streamed

            return objectMapper.writeValueAsString(root);

        } catch (Exception e) {
            throw new RuntimeException("Failed to build Groq request payload", e);
        }
    }

    /**
     * Parse the Groq API JSON response and extract the report text + token usage.
     *
     * Groq response structure (OpenAI-compatible):
     * {
     *   "id": "...",
     *   "model": "llama-3.3-70b-versatile",
     *   "choices": [
     *     {
     *       "index": 0,
     *       "message": { "role": "assistant", "content": "<the report text>" },
     *       "finish_reason": "stop"
     *     }
     *   ],
     *   "usage": {
     *     "prompt_tokens": 512,
     *     "completion_tokens": 820,
     *     "total_tokens": 1332
     *   }
     * }
     */
    private GroqResponse parseGroqResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // Extract generated text from choices[0].message.content
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("Groq returned no choices in response");
            }

            String reportText = choices.get(0)
                .path("message")
                .path("content")
                .asText("No report generated");

            // Extract token usage for monitoring/display
            JsonNode usage         = root.path("usage");
            int promptTokens       = usage.path("prompt_tokens").asInt(0);
            int completionTokens   = usage.path("completion_tokens").asInt(0);
            String modelUsed       = root.path("model").asText(model);

            log.info("Groq report generated: {} prompt tokens, {} completion tokens, model={}",
                promptTokens, completionTokens, modelUsed);

            return new GroqResponse(reportText, promptTokens, completionTokens, modelUsed);

        } catch (Exception e) {
            log.error("Failed to parse Groq response: {}", responseBody, e);
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage(), e);
        }
    }
}
