package com.support.agent.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory per-session state.
 * Stores the message history in the exact format the Anthropic API expects,
 * plus a separate tool-trace list for the React UI.
 */
@Service
public class ContextManager {

    // sessionId → ordered list of Anthropic-format message maps
    private final Map<String, List<Map<String, Object>>> histories =
            new ConcurrentHashMap<>();

    // sessionId → ordered list of tool calls made this session
    private final Map<String, List<Map<String, Object>>> toolTraces =
            new ConcurrentHashMap<>();

    // ── Message history ───────────────────────────────────────────────────

    public void addUserMessage(String sessionId, String text) {
        history(sessionId).add(Map.of("role", "user", "content", text));
    }

    public void addAssistantMessage(String sessionId, String text) {
        history(sessionId).add(Map.of("role", "assistant", "content", text));
    }

    /**
     * Adds the tool use block (from Claude) and the tool result block
     * as a pair — the Anthropic API requires both in the conversation history.
     */
    public void addToolResult(String sessionId,
                              String toolUseId,
                              String toolName,
                              Map<String, Object> args,
                              Object result) {
        List<Map<String, Object>> hist = history(sessionId);

        // 1. Assistant turn: the tool_use block Claude emitted
        hist.add(Map.of(
                "role", "assistant",
                "content", List.of(Map.of(
                        "type",  "tool_use",
                        "id",    toolUseId,
                        "name",  toolName,
                        "input", args
                ))
        ));

        // 2. User turn: the tool_result block we're returning
        hist.add(Map.of(
                "role", "user",
                "content", List.of(Map.of(
                        "type",        "tool_result",
                        "tool_use_id", toolUseId,
                        "content",     String.valueOf(result)
                ))
        ));

        // 3. Add to the UI-facing trace
        trace(sessionId).add(Map.of(
                "tool",   toolName,
                "args",   args,
                "result", result
        ));
    }

    public List<Map<String, Object>> getHistory(String sessionId) {
        return Collections.unmodifiableList(history(sessionId));
    }

    public List<Map<String, Object>> getToolTrace(String sessionId) {
        return Collections.unmodifiableList(trace(sessionId));
    }

    public void clearSession(String sessionId) {
        histories.remove(sessionId);
        toolTraces.remove(sessionId);
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private List<Map<String, Object>> history(String sessionId) {
        return histories.computeIfAbsent(sessionId, k -> new ArrayList<>());
    }

    private List<Map<String, Object>> trace(String sessionId) {
        return toolTraces.computeIfAbsent(sessionId, k -> new ArrayList<>());
    }
}
