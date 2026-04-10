package com.support.agent.controller;

import com.support.agent.service.AgentOrchestrator;
import com.support.agent.service.ContextManager;
import com.support.agent.service.PolicyEngine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    @Autowired private AgentOrchestrator orchestrator;
    @Autowired private ContextManager    contextManager;
    @Autowired private PolicyEngine      policyEngine;

    /**
     * Main chat endpoint.
     * POST /api/agent/chat
     * Body: { "sessionId": "...", "message": "..." }
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request) {

        AgentOrchestrator.AgentResponse result =
                orchestrator.process(
                        request.getSessionId(),
                        request.getMessage());

        return ResponseEntity.ok(new ChatResponse(
                result.reply(),
                result.toolTrace(),
                result.escalated()));
    }

    /**
     * Clear session state (history + policy).
     * DELETE /api/agent/session/{sessionId}
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> clearSession(
            @PathVariable String sessionId) {
        contextManager.clearSession(sessionId);
        policyEngine.clearSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Health check.
     * GET /api/agent/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // ── DTOs ─────────────────────────────────────────────────────────────

    @Data
    public static class ChatRequest {
        @NotBlank
        private String sessionId;
        @NotBlank
        private String message;
    }

    public record ChatResponse(
            String reply,
            List<Map<String, Object>> toolTrace,
            boolean escalated
    ) {}
}
