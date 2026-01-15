CREATE TABLE IF NOT EXISTS public.customer (id BIGINT NOT NULL,
                                            name CHARACTER VARYING(255) NOT NULL,
                                            tier_id bigint,
                                            cidrs CHARACTER VARYING(2048)
);

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_unique_name UNIQUE (name);

CREATE SEQUENCE IF NOT EXISTS customer_seq INCREMENT 50 OWNED BY public.customer.id;
SELECT SETVAL('customer_seq', (SELECT COALESCE(MAX(id), 1) FROM customer)::BIGINT);


CREATE TABLE IF NOT EXISTS public.tier (id BIGINT NOT NULL,
                                        name CHARACTER VARYING(255) NOT NULL,
                                        description CHARACTER VARYING(255),
                                        capacity INTEGER NOT NULL,
                                        time INTEGER NOT NULL,
                                        refill_strategy CHARACTER VARYING(255) NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS tier_seq INCREMENT 50 OWNED BY public.tier.id;
SELECT SETVAL('tier_seq', (SELECT COALESCE(MAX(id), 1) FROM tier)::BIGINT);

ALTER TABLE ONLY public.tier
    ADD CONSTRAINT tier_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tier
    ADD CONSTRAINT tier_unique_name UNIQUE (name);

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_tier_id_fk FOREIGN KEY (tier_id) REFERENCES public.tier(id);
