-- publisher_agreement: history of the publisher agreement versions users signed

CREATE SEQUENCE IF NOT EXISTS publisher_agreement_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS public.publisher_agreement
(
    id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('publisher_agreement_seq'),
    -- the history goes with the user
    user_data_id BIGINT NOT NULL REFERENCES public.user_data(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    signed_timestamp TIMESTAMP without time zone NOT NULL,
    -- set when an admin revokes the signature; the row stays as audit trail
    revoked_timestamp TIMESTAMP without time zone,
    -- the admin may be deleted later, the record of the revocation must remain
    revoked_by_id BIGINT REFERENCES public.user_data(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS publisher_agreement__user_data_id__idx ON public.publisher_agreement (user_data_id);
