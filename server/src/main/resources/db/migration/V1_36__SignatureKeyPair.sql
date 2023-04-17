CREATE TABLE signature_key_pair (
    id BIGINT NOT NULL,
    public_id CHARACTER VARYING(128),
    private_key BYTEA NOT NULL,
    public_key_text CHARACTER VARYING(255) NOT NULL,
    created TIMESTAMP WITHOUT TIME ZONE,
    active BOOLEAN NOT NULL,
    CONSTRAINT signature_key_pair_pkey PRIMARY KEY (id)
);

ALTER TABLE extension_version ADD COLUMN signature_key_pair_id BIGINT;
ALTER TABLE ONLY public.extension_version
    ADD CONSTRAINT extension_version_signature_key_pair_fkey FOREIGN KEY (signature_key_pair_id) REFERENCES signature_key_pair(id);
