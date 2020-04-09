-- Create a super user with a personal access token `super_token` for publishing with the CLI.
INSERT INTO user_data (id, login_name) VALUES (1001, 'super_user');
INSERT INTO personal_access_token (id, user_data, value, active, created_timestamp, accessed_timestamp, description) VALUES (1001, 1001, 'super_token', true, current_timestamp, current_timestamp, 'For publishing test extensions');
