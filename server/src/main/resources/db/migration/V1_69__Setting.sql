-- setting table

CREATE SEQUENCE IF NOT EXISTS setting_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS public.setting
(
    id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('setting_seq'),
    key CHARACTER VARYING(255) NOT NULL,
    value JSONB NOT NULL,
    created_at TIMESTAMP without time zone,
    updated_at TIMESTAMP without time zone,

    -- constraints

    CONSTRAINT setting_key_unique UNIQUE (key)
);
