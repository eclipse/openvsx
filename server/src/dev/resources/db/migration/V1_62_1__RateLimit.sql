-- add basic tier and assign customer for loopback IP to it

INSERT INTO tier (id, name, description, tier_type, capacity, duration, refill_strategy) VALUES (1, 'free', '', 'FREE', 50, 60, 'GREEDY');
INSERT INTO tier (id, name, description, tier_type, capacity, duration, refill_strategy) VALUES (2, 'safety', '', 'SAFETY', 200, 60, 'GREEDY');
INSERT INTO tier (id, name, description, tier_type, capacity, duration, refill_strategy) VALUES (3, 'basic', '', 'NON_FREE', 100, 60, 'GREEDY');
INSERT INTO customer (id, name, tier_id, state, cidr_blocks) VALUES (1, 'loopback', 3, 'EVALUATION', '127.0.0.1/32');
