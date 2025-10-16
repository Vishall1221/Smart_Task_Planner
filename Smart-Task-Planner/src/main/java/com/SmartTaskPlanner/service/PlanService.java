package com.SmartTaskPlanner.service;

import com.SmartTaskPlanner.model.Plan;
import com.SmartTaskPlanner.model.Task;
import com.SmartTaskPlanner.repository.PlanRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PlanService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;


    private static final String GEMINI_MODEL = "gemini-2.0-flash";

    // Build the URL with the key as a query param (most reliable)
    private String geminiUrl() {
        return "https://generativelanguage.googleapis.com/v1/models/"
                + GEMINI_MODEL + ":generateContent?key=" + geminiApiKey;
    }

    private final PlanRepository planRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public Plan createPlanFromGoal(String goal) {
        String prompt = buildPrompt(goal);
        String response = callGeminiAPI(prompt);
        List<Task> tasks = parseTasks(response);

        Plan plan = new Plan();
        plan.setGoal(goal);
        plan.setCreatedAt(LocalDateTime.now());
        plan.setTasks(tasks);
        tasks.forEach(t -> t.setPlan(plan)); // link back

        return planRepository.save(plan);
    }

    private String buildPrompt(String goal) {
        // Keep it super explicit so model returns clean JSON
        return "You are a task planning assistant.\n"
                + "Break down this goal into actionable tasks with an estimated duration "
                + "and dependencies (which tasks must be done first).\n"
                + "Goal: \"" + goal + "\"\n\n"
                + "Return ONLY a valid JSON array (no prose, no markdown), where each object has:\n"
                + "{\"description\":\"string\",\"duration\":\"string\",\"dependencies\":\"string or empty\"}\n";
    }

    private String callGeminiAPI(String prompt) {
        RestTemplate restTemplate = new RestTemplate();

        String body = "{ \"contents\": [ { \"parts\": [ { \"text\": \"" +
                escapeForJson(prompt) + "\" } ] } ] }";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // (Using key in URL, so header not required)
        // headers.set("x-goog-api-key", geminiApiKey);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        String url = geminiUrl();

        try {
            String resp = restTemplate.postForObject(url, entity, String.class);
            System.out.println("Gemini raw response: " + resp);
            return resp;
        } catch (HttpClientErrorException e) {
            System.err.println("Gemini HTTP error: " + e.getStatusCode());
            System.err.println("Gemini error body: " + e.getResponseBodyAsString());
            throw e;
        }
    }

    private List<Task> parseTasks(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            System.err.println("Empty response from Gemini.");
            return Collections.emptyList();
        }
        try {
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                System.err.println("No candidates in Gemini response.");
                return Collections.emptyList();
            }

            // Take first candidate text
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                System.err.println("No parts in Gemini response.");
                return Collections.emptyList();
            }

            String text = parts.get(0).path("text").asText("");
            if (text.isEmpty()) {
                System.err.println("Empty text in Gemini response.");
                return Collections.emptyList();
            }

            // Some models sometimes wrap JSON in prose or code fences; try to extract the JSON array
            String jsonArray = extractFirstJsonArray(text);
            if (jsonArray == null) {
                // Try to parse whatever we got (if it's already pure JSON)
                if (text.trim().startsWith("[")) {
                    jsonArray = text.trim();
                } else {
                    System.err.println("Could not find JSON array in model output.");
                    return Collections.emptyList();
                }
            }

            // Map to our Task POJO list
            List<Task> tasks = mapper.readValue(jsonArray, new TypeReference<List<Task>>() {});
            // Ensure non-null list
            return tasks != null ? tasks : new ArrayList<>();

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // -------- helpers --------

    private static String escapeForJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Extracts the first JSON array substring from a blob of text (handles code fences too).
     */
    private static String extractFirstJsonArray(String text) {
        // Strip markdown fences if present
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            // remove leading ```json / ``` and trailing ```
            cleaned = cleaned.replaceFirst("^```(json|JSON)?\\s*", "");
            int fence = cleaned.lastIndexOf("```");
            if (fence >= 0) cleaned = cleaned.substring(0, fence);
            cleaned = cleaned.trim();
        }

        // Find the first [...] block (rudimentary but effective for our use)
        int start = cleaned.indexOf('[');
        if (start < 0) return null;
        int depth = 0;
        for (int i = start; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return cleaned.substring(start, i + 1);
                }
            }
        }
        return null;
    }
}
