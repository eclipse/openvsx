-- Create a super user with a personal access token `super_token` for publishing with the CLI.
INSERT INTO user_data (id, login_name) VALUES (1001, 'super_user');
INSERT INTO personal_access_token (id, user_data, value, created_timestamp, accessed_timestamp, description) VALUES (1001, 1001, 'super_token', current_timestamp, current_timestamp, 'For publishing test extensions');

-- Create all required publishers for the downloaded test extensions and assign `super_user` as owner.

INSERT INTO publisher (id, name) VALUES (1001, 'DotJoshJohnson');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (1001, 1001, 1001, 'owner');

INSERT INTO publisher (id, name) VALUES (1002, 'eamodio');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (1002, 1002, 1001, 'owner');

INSERT INTO publisher (id, name) VALUES (1003, 'Equinusocio');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (1003, 1003, 1001, 'owner');

INSERT INTO publisher (id, name) VALUES (1004, 'felixfbecker');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (1004, 1004, 1001, 'owner');

INSERT INTO publisher (id, name) VALUES (1005, 'formulahendry');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (1005, 1005, 1001, 'owner');

INSERT INTO publisher (id, name) VALUES (1006, 'HookyQR');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (1006, 1006, 1001, 'owner');

INSERT INTO publisher (id, name) VALUES (1007, 'ms-azuretools');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (1007, 1007, 1001, 'owner');

INSERT INTO publisher (id, name) VALUES (1008, 'ms-mssql');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (1008, 1008, 1001, 'owner');

INSERT INTO publisher (id, name) VALUES (1009, 'ms-python');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (1009, 1009, 1001, 'owner');

INSERT INTO publisher (id, name) VALUES (1010, 'ms-vscode');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (1010, 1010, 1001, 'owner');

INSERT INTO publisher (id, name) VALUES (1011, 'octref');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (1011, 1011, 1001, 'owner');

INSERT INTO publisher (id, name) VALUES (1012, 'redhat');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (1012, 1012, 1001, 'owner');

INSERT INTO publisher (id, name) VALUES (1013, 'ritwickdey');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (1013, 1013, 1001, 'owner');

INSERT INTO publisher (id, name) VALUES (1014, 'sburg');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (1014, 1014, 1001, 'owner');

INSERT INTO publisher (id, name) VALUES (1015, 'Wscats');
INSERT INTO publisher_membership (id, publisher, user_data, role) VALUES (1015, 1015, 1001, 'owner');
