package com.support.agent.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.agent.model.Escalation;
import com.support.agent.repository.EscalationRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class EscalateToHumanTool implements McpTool {

    @Autowired
    private EscalationRepository escalationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String name() {
        return "escalate_to_human";
    }

    @Override
    public String description() {
        return "Escalate a case to a human support agent when the request is ambiguous, " +
               "outside policy, or requires judgement beyond the agent's authority. " +
               "Packages full context and persists a ticket to the escalations table. " +
               "Call this instead of guessing.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "required", List.of("session_id", "reason"),
                "properties", Map.of(
                        "session_id", Map.of(
                                "type", "string",
                                "description", "The current session ID"
                        ),
                        "customer_id", Map.of(
                                "type", "string",
                                "description", "The customer's ID if known"
                        ),
                        "reason", Map.of(
                                "type", "string",
                                "description", "Why this case is being escalated"
                        ),
                        "summary", Map.of(
                                "type", "string",
                                "description", "Brief summary of the issue for the human agent"
                        )
                )
        );
    }

    @Override
    @Transactional
    public Object execute(Map<String, Object> args) {
        Escalation escalation = new Escalation();
        escalation.setSessionId((String) args.get("session_id"));
        escalation.setCustomerId((String) args.get("customer_id"));
        escalation.setReason((String) args.get("reason"));
        escalation.setCreatedAt(LocalDateTime.now());
        escalation.setResolved(false);

        // Serialise full args as context for the human agent
        try {
            escalation.setContextJson(objectMapper.writeValueAsString(args));
        } catch (JsonProcessingException e) {
            escalation.setContextJson("{}");
        }

        Escalation saved = escalationRepository.save(escalation);

        return Map.of(
                "status",    "escalated",
                "ticket_id", "TKT-" + saved.getId(),
                "message",   "Your case has been escalated. A human agent will follow up within 2 hours.",
                "reason",    args.getOrDefault("reason", "")
        );
    }
}
