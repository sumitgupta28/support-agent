package com.support.agent.mcp;

import com.support.agent.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Component
public class LookupOrderTool implements McpTool {

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public String name() {
        return "lookup_order";
    }

    @Override
    public String description() {
        return "Fetch order details including status, amount, and refund eligibility. " +
               "Always call this before attempting a refund. " +
               "Optionally pass customer_id to validate the order belongs to that customer.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "required", List.of("order_id"),
                "properties", Map.of(
                        "order_id", Map.of(
                                "type", "string",
                                "description", "The order identifier (e.g. ORD-101)"
                        ),
                        "customer_id", Map.of(
                                "type", "string",
                                "description", "Optional: customer ID to verify order ownership"
                        )
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> args) {
        String orderId    = (String) args.get("order_id");
        String customerId = (String) args.get("customer_id");

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("order_id is required");
        }

        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No order found with ID: " + orderId));

        // Security guard — verify the order belongs to the stated customer
        if (customerId != null && !customerId.isBlank()
                && !order.getCustomerId().equals(customerId)) {
            throw new SecurityException(
                    "Order " + orderId + " does not belong to customer " + customerId);
        }

        return order;
    }
}
