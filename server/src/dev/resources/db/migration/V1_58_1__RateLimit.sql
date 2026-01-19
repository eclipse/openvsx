-- add basic tier and assign customer for loopback IP to it

INSERT INTO tier (id, name, description, capacity, duration, refill_strategy) VALUES (1, 'basic', '', 100, 60, 'GREEDY');
INSERT INTO customer (id, name, tier_id, cidr_blocks) VALUES (1, 'loopback', 1, '127.0.0.1/32');
