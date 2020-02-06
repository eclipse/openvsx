-- Create a super user with a personal access token `super_token` for publishing with the CLI.
INSERT INTO user_data (id, login_name) VALUES (1, 'super_user');
INSERT INTO personal_access_token (id, user_data, value, created_timestamp, accessed_timestamp, description) VALUES (1, 1, 'super_token', current_timestamp, current_timestamp, 'For publishing test extensions');

-- Create all required publishers for the downloaded test extensions and assign `super_user` as owner.

INSERT INTO publisher (id, name) VALUES (1, 'DotJoshJohnson');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (1, 1, 1, 'owner');

INSERT INTO publisher (id, name) VALUES (2, 'eamodio');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (2, 2, 1, 'owner');

INSERT INTO publisher (id, name) VALUES (3, 'Equinusocio');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (3, 3, 1, 'owner');

INSERT INTO publisher (id, name) VALUES (4, 'felixfbecker');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (4, 4, 1, 'owner');

INSERT INTO publisher (id, name) VALUES (5, 'formulahendry');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (5, 5, 1, 'owner');

INSERT INTO publisher (id, name) VALUES (6, 'HookyQR');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (6, 6, 1, 'owner');

INSERT INTO publisher (id, name) VALUES (7, 'ms-azuretools');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (7, 7, 1, 'owner');

INSERT INTO publisher (id, name) VALUES (8, 'ms-mssql');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (8, 8, 1, 'owner');

INSERT INTO publisher (id, name) VALUES (9, 'ms-python');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (9, 9, 1, 'owner');

INSERT INTO publisher (id, name) VALUES (10, 'ms-vscode');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (10, 10, 1, 'owner');

INSERT INTO publisher (id, name) VALUES (11, 'octref');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (11, 11, 1, 'owner');

INSERT INTO publisher (id, name) VALUES (12, 'redhat');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (12, 12, 1, 'owner');

INSERT INTO publisher (id, name) VALUES (13, 'ritwickdey');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (13, 13, 1, 'owner');

INSERT INTO publisher (id, name) VALUES (14, 'sburg');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (14, 14, 1, 'owner');

INSERT INTO publisher (id, name) VALUES (15, 'Wscats');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (15, 15, 1, 'owner');
