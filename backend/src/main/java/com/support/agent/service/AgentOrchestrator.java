package com.support.agent.service;

import com.support.agent.mcp.McpToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Core ReAct loop:
 *   Receive user message
 *   → Call Claude with history + tool schemas
 *   → If Claude picks a tool: policy-check → execute → add result → loop
 *   → If Claude gives a text reply: return it
 *   → If max iterations hit: escalate
 */
@Service
@Slf4j
public class AgentOrchestrator {

    @Autowired private ClaudeApiClient claudeClient;
    @Autowired private McpToolRegistry toolRegistry;
    @Autowired private ContextManager  contextManager;
    @Autowired private PolicyEngine    policyEngine;

    @Value("${agent.max-iterations:6}")
    private int maxIterations;

    private static final String SYSTEM_PROMPT = """
            You are a customer support agent for an e-commerce platform.
            You have access to four tools: get_customer, lookup_order,
            process_refund, and escalate_to_human.

            Rules you must follow:
            1. Always call get_customer first to verify the customer's identity.
            2. Always call lookup_order before calling process_refund.
            3. If a case is ambiguous, outside policy, or you are unsure,
               call escalate_to_human — do not guess.
            4. If the order is outside the 30-day return window, escalate.
            5. Be concise, empathetic, and professional in all responses.
            """;

    // ── Public entry point ────────────────────────────────────────────────

    public AgentResponse process(String sessionId, String userMessage) {
        log.info("[{}] User: {}", sessionId, userMessage);
        contextManager.addUserMessage(sessionId, userMessage);

        List<Map<String, Object>> toolTrace = new ArrayList<>();

        for (int iteration = 0; iteration < maxIterations; iteration++) {

            ClaudeApiClient.ClaudeResponse claudeResp = claudeClient.complete(
                    SYSTEM_PROMPT,
                    contextManager.getHistory(sessionId),
                    toolRegistry.getSchemas()
            );

            // ── Tool use branch ───────────────────────────────────────────
            if (claudeResp.isToolUse()) {
                String toolName             = claudeResp.getToolName();
                String toolUseId            = claudeResp.getToolUseId();
                Map<String, Object> toolArgs = claudeResp.getToolInput();

                log.info("[{}] Tool call: {} args={}", sessionId, toolName, toolArgs);

                // Policy gate — block if ordering rules are violated
                if (!policyEngine.allow(sessionId, toolName, toolArgs)) {
                    log.warn("[{}] Policy blocked tool: {}", sessionId, toolName);
                    String reason = "Policy violation: called " + toolName
                            + " out of order.";
                    return AgentResponse.escalated(reason, toolTrace);
                }

                Object result;
                try {
                    result = toolRegistry.invoke(toolName, toolArgs);
                    policyEngine.recordInvocation(sessionId, toolName);
                    log.info("[{}] Tool result: {}", sessionId, result);
                } catch (Exception ex) {
                    log.error("[{}] Tool error: {}", sessionId, ex.getMessage());
                    result = Map.of("error", ex.getMessage());
                }

                // Record in context (both as API history and as UI trace)
                contextManager.addToolResult(
                        sessionId, toolUseId, toolName, toolArgs, result);

                toolTrace.add(Map.of(
                        "tool",   toolName,
                        "args",   toolArgs,
                        "result", result
                ));

                continue; // loop — give Claude the tool result
            }

            // ── Final text reply ──────────────────────────────────────────
            String reply = claudeResp.getText();
            log.info("[{}] Agent reply: {}", sessionId, reply);
            contextManager.addAssistantMessage(sessionId, reply);

            boolean escalated = toolTrace.stream()
                    .anyMatch(t -> "escalate_to_human"
                            .equals(t.get("tool")));

            return new AgentResponse(reply, toolTrace, escalated);
        }

        // Exceeded iteration budget
        log.warn("[{}] Max iterations reached", sessionId);
        return AgentResponse.escalated(
                "I wasn't able to resolve your request automatically. "
                + "A human agent will follow up shortly.", toolTrace);
    }

    // ── Response DTO ──────────────────────────────────────────────────────

    public record AgentResponse(
            String reply,
            List<Map<String, Object>> toolTrace,
            boolean escalated
    ) {
        public static AgentResponse escalated(String reason,
                                              List<Map<String, Object>> trace) {
            return new AgentResponse(reason, trace, true);
        }
    }
}
