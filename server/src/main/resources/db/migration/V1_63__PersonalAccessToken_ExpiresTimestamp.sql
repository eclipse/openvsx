-- add new columns wrt access token expiration
ALTER TABLE personal_access_token
    ADD COLUMN expires_timestamp TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN notified BOOLEAN;

UPDATE personal_access_token
    SET expires_timestamp = NULL, notified = FALSE;
