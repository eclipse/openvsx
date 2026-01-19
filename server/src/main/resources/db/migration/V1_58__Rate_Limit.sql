-- create tier table
CREATE TABLE IF NOT EXISTS public.tier (id BIGINT NOT NULL,
                                        name CHARACTER VARYING(255) NOT NULL,
                                        description CHARACTER VARYING(255),
                                        capacity INTEGER NOT NULL,
                                        duration INTEGER NOT NULL,
                                        refill_strategy CHARACTER VARYING(255) NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS tier_seq INCREMENT 50 OWNED BY public.tier.id;
SELECT SETVAL('tier_seq', (SELECT COALESCE(MAX(id), 1) FROM tier)::BIGINT);

ALTER TABLE ONLY public.tier
    ADD CONSTRAINT tier_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tier
    ADD CONSTRAINT tier_unique_name UNIQUE (name);

-- create customer table
CREATE TABLE IF NOT EXISTS public.customer (id BIGINT NOT NULL,
                                            name CHARACTER VARYING(255) NOT NULL,
                                            tier_id bigint,
                                            state CHARACTER VARYING(255) NOT NULL,
                                            cidr_blocks CHARACTER VARYING(2048)
);

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_unique_name UNIQUE (name);

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_tier_id_fk FOREIGN KEY (tier_id) REFERENCES public.tier(id);

CREATE SEQUENCE IF NOT EXISTS customer_seq INCREMENT 50 OWNED BY public.customer.id;
SELECT SETVAL('customer_seq', (SELECT COALESCE(MAX(id), 1) FROM customer)::BIGINT);

-- create usage_stats table
CREATE TABLE IF NOT EXISTS public.usage_stats (id BIGINT NOT NULL,
                                               customer_id BIGINT,
                                               window_start TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                               duration INTEGER NOT NULL,
                                               count BIGINT NOT NULL
);

ALTER TABLE ONLY public.usage_stats
    ADD CONSTRAINT usage_stats_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.usage_stats
    ADD CONSTRAINT usage_stats_unique_customer_window UNIQUE (customer_id, window_start);

ALTER TABLE ONLY public.usage_stats
    ADD CONSTRAINT usage_stats_customer_id_fk FOREIGN KEY (customer_id) REFERENCES public.customer(id);

CREATE SEQUENCE IF NOT EXISTS usage_stats_seq INCREMENT 50 OWNED BY public.usage_stats.id;
SELECT SETVAL('usage_stats_seq', (SELECT COALESCE(MAX(id), 1) FROM usage_stats)::BIGINT);
