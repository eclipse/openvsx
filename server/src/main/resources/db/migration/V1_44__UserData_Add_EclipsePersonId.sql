ALTER TABLE user_data ADD COLUMN eclipse_person_id CHARACTER VARYING(255);
UPDATE user_data SET eclipse_person_id = eclipse_data::json->'personId';
ALTER TABLE user_data DROP COLUMN eclipse_data;