package com.support.agent.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces business rules before any MCP tool is invoked.
 *
 * Rules applied here:
 *  1. get_customer must be called before any mutation tool.
 *  2. lookup_order must be called before process_refund.
 *  3. process_refund may not be retried if it already succeeded this session.
 */
@Service
public class PolicyEngine {

    // sessionId → set of tools already invoked successfully
    private final Map<String, Set<String>> invokedTools = new ConcurrentHashMap<>();

    // Mutation tools that require prior identity verification
    private static final Set<String> REQUIRES_CUSTOMER = Set.of(
            "lookup_order", "process_refund", "escalate_to_human"
    );

    // Mutation tools that require an order to be looked up first
    private static final Set<String> REQUIRES_ORDER = Set.of(
            "process_refund"
    );

    /**
     * Returns true if the tool is allowed to run given current session state.
     * Returns false (block) if a policy rule is violated.
     */
    public boolean allow(String sessionId, String toolName, Map<String, Object> args) {

        Set<String> called = invokedTools.getOrDefault(sessionId, Set.of());

        // Rule 1: identity must be verified before mutation tools
        if (REQUIRES_CUSTOMER.contains(toolName) && !called.contains("get_customer")) {
            return false;
        }

        // Rule 2: order must be fetched before attempting a refund
        if (REQUIRES_ORDER.contains(toolName) && !called.contains("lookup_order")) {
            return false;
        }

        // Rule 3: prevent duplicate refund within same session
        if ("process_refund".equals(toolName) && called.contains("process_refund")) {
            return false;
        }

        return true;
    }

    /** Called by AgentOrchestrator after a successful tool execution. */
    public void recordInvocation(String sessionId, String toolName) {
        invokedTools
                .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(toolName);
    }

    public void clearSession(String sessionId) {
        invokedTools.remove(sessionId);
    }
}
