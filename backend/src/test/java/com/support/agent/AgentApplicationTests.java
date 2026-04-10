package com.support.agent;

import com.support.agent.controller.AgentController;
import com.support.agent.model.Customer;
import com.support.agent.model.Order;
import com.support.agent.repository.CustomerRepository;
import com.support.agent.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AgentApplicationTests {

    @Autowired CustomerRepository customerRepository;
    @Autowired OrderRepository    orderRepository;

    @BeforeEach
    void seedTestData() {
        Customer c = new Customer();
        c.setId("C001");
        c.setName("Alice Chen");
        c.setEmail("alice@example.com");
        c.setTier("gold");
        c.setCreatedAt(LocalDateTime.now());
        customerRepository.save(c);

        Order o = new Order();
        o.setId("ORD-101");
        o.setCustomerId("C001");
        o.setAmount(new BigDecimal("89.99"));
        o.setStatus("delivered");
        o.setCreatedAt(LocalDateTime.now().minusDays(5));
        o.setRefundEligible(true);
        o.setRefunded(false);
        orderRepository.save(o);
    }

    @Test
    void contextLoads() {
        // Verifies the Spring context starts without errors
    }

    @Test
    void customerIsPersistedCorrectly() {
        Optional<Customer> customer = customerRepository.findById("C001");
        assertThat(customer).isPresent();
        assertThat(customer.get().getName()).isEqualTo("Alice Chen");
        assertThat(customer.get().getTier()).isEqualTo("gold");
    }

    @Test
    void orderRefundEligibilityIsCorrect() {
        Optional<Order> order = orderRepository.findById("ORD-101");
        assertThat(order).isPresent();
        assertThat(order.get().isRefundEligible()).isTrue();
        assertThat(order.get().isRefunded()).isFalse();
    }

    @Test
    void refundPersistsToDatabase() {
        Order order = orderRepository.findById("ORD-101").orElseThrow();
        order.setRefunded(true);
        orderRepository.save(order);

        Order reloaded = orderRepository.findById("ORD-101").orElseThrow();
        assertThat(reloaded.isRefunded()).isTrue();
    }
}
