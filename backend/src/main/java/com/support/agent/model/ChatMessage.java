package com.support.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

// ── Inbound DTO ──────────────────────────────────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
class ChatRequest {
    private String sessionId;
    private String message;
}

// ── Tool call trace entry ────────────────────────────────────
@Data
@AllArgsConstructor
class ToolCall {
    private String tool;
    private Map<String, Object> args;
    private Object result;
}

// ── Outbound DTO ─────────────────────────────────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
class ChatResponse {
    private String reply;
    private List<ToolCall> toolTrace;
    private boolean escalated;

    public static ChatResponse resolved(String reply, List<ToolCall> trace) {
        boolean escalated = trace.stream()
                .anyMatch(t -> "escalate_to_human".equals(t.getTool()));
        return new ChatResponse(reply, trace, escalated);
    }

    public static ChatResponse escalated(String reason, List<ToolCall> trace) {
        return new ChatResponse(reason, trace, true);
    }
}
