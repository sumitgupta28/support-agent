INSERT INTO customers (id, name, email, tier) VALUES
    ('C001', 'Alice Chen',  'alice@example.com', 'gold'),
    ('C002', 'Bob Patel',   'bob@example.com',   'standard'),
    ('C003', 'Carol Smith', 'carol@example.com', 'silver');

INSERT INTO orders (id, customer_id, amount, status, created_at, refund_eligible, refunded) VALUES
    -- eligible: delivered 5 days ago → happy path
    ('ORD-101', 'C001', 89.99,  'delivered', NOW() - INTERVAL '5 days',  TRUE,  FALSE),
    -- ineligible: 45 days old → escalation path
    ('ORD-102', 'C002', 149.00, 'delivered', NOW() - INTERVAL '45 days', FALSE, FALSE),
    -- already refunded → edge case
    ('ORD-103', 'C003', 59.99,  'delivered', NOW() - INTERVAL '10 days', TRUE,  TRUE);
