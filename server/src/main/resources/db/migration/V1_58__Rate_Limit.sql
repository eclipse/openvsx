CREATE TABLE IF NOT EXISTS public.customer (id BIGINT NOT NULL,
                                            name CHARACTER VARYING(255) NOT NULL,
                                            cidrs CHARACTER VARYING(2048)
);

CREATE SEQUENCE IF NOT EXISTS customer_seq INCREMENT 50 OWNED BY public.customer.id;
SELECT SETVAL('customer_seq', (SELECT COALESCE(MAX(id), 1) FROM customer)::BIGINT);
