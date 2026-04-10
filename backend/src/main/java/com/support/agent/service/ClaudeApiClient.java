package com.support.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Thin wrapper around the Anthropic /v1/messages endpoint.
 * Uses OkHttp for the HTTP call and Jackson for JSON.
 */
@Service
public class ClaudeApiClient {

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.model}")
    private String model;

    @Value("${anthropic.max-tokens}")
    private int maxTokens;
    @Value("${anthropic.api-url}")
    private String API_URL;

    private static final MediaType JSON =
            MediaType.get("application/json");

    private final OkHttpClient http = new OkHttpClient.Builder()
            .callTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    // ── Public API ────────────────────────────────────────────────────────

    public ClaudeResponse complete(
            String systemPrompt,
            List<Map<String, Object>> history,
            List<Map<String, Object>> toolSchemas) {

        try {
            Map<String, Object> body = buildBody(
                    systemPrompt, history, toolSchemas);
            String requestJson = mapper.writeValueAsString(body);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .post(RequestBody.create(requestJson, JSON))
                    .build();

            try (Response response = http.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null
                            ? response.body().string() : "(no body)";
                    throw new RuntimeException(
                            "Anthropic API error " + response.code()
                            + ": " + errorBody);
                }
                String responseJson = response.body().string();
                return parseResponse(mapper.readTree(responseJson));
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to call Anthropic API", e);
        }
    }

    // ── Request builder ───────────────────────────────────────────────────

    private Map<String, Object> buildBody(
            String systemPrompt,
            List<Map<String, Object>> history,
            List<Map<String, Object>> toolSchemas) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model",      model);
        body.put("max_tokens", maxTokens);
        body.put("system",     systemPrompt);
        body.put("messages",   history);

        if (toolSchemas != null && !toolSchemas.isEmpty()) {
            body.put("tools", toolSchemas);
        }

        return body;
    }

    // ── Response parser ───────────────────────────────────────────────────

    private ClaudeResponse parseResponse(JsonNode root) {
        String stopReason = root.path("stop_reason").asText();

        // Claude wants to use a tool
        if ("tool_use".equals(stopReason)) {
            JsonNode content = root.path("content");
            for (JsonNode block : content) {
                if ("tool_use".equals(block.path("type").asText())) {
                    String id   = block.path("id").asText();
                    String name = block.path("name").asText();
                    Map<String, Object> input = mapper.convertValue(
                            block.path("input"),
                            mapper.getTypeFactory()
                                  .constructMapType(Map.class,
                                          String.class, Object.class));
                    return ClaudeResponse.toolUse(id, name, input);
                }
            }
        }

        // Claude returned a final text reply
        JsonNode content = root.path("content");
        StringBuilder text = new StringBuilder();
        for (JsonNode block : content) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }
        return ClaudeResponse.text(text.toString().trim());
    }

    // ── Inner response model ──────────────────────────────────────────────

    public static class ClaudeResponse {
        private final boolean toolUse;
        private final String  toolUseId;
        private final String  toolName;
        private final Map<String, Object> toolInput;
        private final String  text;

        private ClaudeResponse(boolean toolUse, String toolUseId,
                                String toolName,
                                Map<String, Object> toolInput, String text) {
            this.toolUse   = toolUse;
            this.toolUseId = toolUseId;
            this.toolName  = toolName;
            this.toolInput = toolInput;
            this.text      = text;
        }

        public static ClaudeResponse toolUse(String id, String name,
                                             Map<String, Object> input) {
            return new ClaudeResponse(true, id, name, input, null);
        }

        public static ClaudeResponse text(String text) {
            return new ClaudeResponse(false, null, null, null, text);
        }

        public boolean isToolUse()              { return toolUse; }
        public String  getToolUseId()           { return toolUseId; }
        public String  getToolName()            { return toolName; }
        public Map<String, Object> getToolInput() { return toolInput; }
        public String  getText()                { return text; }
    }
}
