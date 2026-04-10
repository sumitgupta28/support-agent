package com.support.agent.mcp;

import com.support.agent.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Component
public class ProcessRefundTool implements McpTool {

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public String name() {
        return "process_refund";
    }

    @Override
    public String description() {
        return "Issue a refund for an order. Validates eligibility server-side — " +
               "never trust the agent's prior check alone. " +
               "Requires order_id, customer_id, and a reason string. " +
               "Always call lookup_order first before calling this tool.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "required", List.of("order_id", "customer_id", "reason"),
                "properties", Map.of(
                        "order_id", Map.of(
                                "type", "string",
                                "description", "The order to refund"
                        ),
                        "customer_id", Map.of(
                                "type", "string",
                                "description", "The customer requesting the refund"
                        ),
                        "reason", Map.of(
                                "type", "string",
                                "description", "Brief reason for the refund (e.g. duplicate charge, damaged item)"
                        )
                )
        );
    }

    @Override
    @Transactional  // PostgreSQL rolls back if anything throws after repo.save()
    public Object execute(Map<String, Object> args) {
        String orderId    = (String) args.get("order_id");
        String customerId = (String) args.get("customer_id");
        String reason     = (String) args.get("reason");

        if (orderId == null || customerId == null) {
            throw new IllegalArgumentException("order_id and customer_id are required");
        }

        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Order not found: " + orderId));

        // ── Server-side guardrails ─────────────────────────────────────────
        // These checks run even if the agent already called lookup_order,
        // because the agent's tool result cache could be stale.

        if (!order.getCustomerId().equals(customerId)) {
            return Map.of("status", "denied",
                    "reason", "Order does not belong to the stated customer");
        }

        if (!order.isRefundEligible()) {
            return Map.of("status", "denied",
                    "reason", "Order is outside the 30-day return window");
        }

        if (order.isRefunded()) {
            return Map.of("status", "denied",
                    "reason", "A refund has already been issued for this order");
        }

        // ── Execute ───────────────────────────────────────────────────────
        order.setRefunded(true);
        orderRepository.save(order);   // persisted to PostgreSQL

        String confirmationId = "REF-" + UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase();

        return Map.of(
                "status",          "refunded",
                "order_id",        orderId,
                "amount",          order.getAmount(),
                "confirmation_id", confirmationId,
                "reason",          reason
        );
    }
}
