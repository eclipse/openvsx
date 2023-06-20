-- unique file resources
DELETE FROM file_resource fr
USING file_resource ufr
WHERE fr.id > ufr.id
AND fr.extension_id = ufr.extension_id
AND fr.type = ufr.type
AND UPPER(fr.name) = UPPER(ufr.name);

CREATE UNIQUE INDEX unique_file_resource ON file_resource(extension_id, type, UPPER(name));

-- unique user data
CREATE TEMP TABLE tmp_user_data AS
SELECT g.first_id, u.id other_id
FROM (
    SELECT provider, login_name, MIN(id) first_id
    FROM user_data
    GROUP BY provider, login_name
) g
JOIN user_data u ON (u.provider = g.provider OR (u.provider IS NULL AND g.provider IS NULL)) AND u.login_name = g.login_name
WHERE g.first_id <> u.id;

UPDATE extension_review
SET user_id = d.first_id
FROM tmp_user_data d
WHERE user_id = d.other_id;

UPDATE namespace_membership
SET user_data = d.first_id
FROM tmp_user_data d
WHERE user_data = d.other_id;

UPDATE persisted_log
SET user_data = d.first_id
FROM tmp_user_data d
WHERE user_data = d.other_id;

UPDATE personal_access_token
SET user_data = d.first_id
FROM tmp_user_data d
WHERE user_data = d.other_id;

DELETE FROM user_data u
USING tmp_user_data tu
WHERE u.id = tu.other_id;

DROP TABLE tmp_user_data;
ALTER TABLE user_data ADD CONSTRAINT unique_user_data UNIQUE (provider, login_name);

-- unique namespace membership
DELETE FROM namespace_membership nm
USING namespace_membership onm
LEFT JOIN (
    SELECT DISTINCT ON (user_data, namespace) id
    FROM namespace_membership
    ORDER BY user_data, namespace, role DESC
) unm ON unm.id = onm.id
WHERE nm.id = onm.id AND unm.id IS NULL;

ALTER TABLE namespace_membership ADD CONSTRAINT unique_namespace_membership UNIQUE (user_data, namespace);

-- unique signature key pair
ALTER TABLE signature_key_pair ADD CONSTRAINT signature_key_pair_unique_public_id UNIQUE (public_id);