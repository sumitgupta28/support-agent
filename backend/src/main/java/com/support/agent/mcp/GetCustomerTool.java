package com.support.agent.mcp;

import com.support.agent.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Component
public class GetCustomerTool implements McpTool {

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    public String name() {
        return "get_customer";
    }

    @Override
    public String description() {
        return "Fetch a customer's profile, tier, and account details by customer ID. " +
               "Always call this first to verify the customer's identity before taking any action.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "required", List.of("customer_id"),
                "properties", Map.of(
                        "customer_id", Map.of(
                                "type", "string",
                                "description", "The unique customer identifier (e.g. C001)"
                        )
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> args) {
        String customerId = (String) args.get("customer_id");
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer_id is required");
        }

        return customerRepository.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No customer found with ID: " + customerId));
    }
}
