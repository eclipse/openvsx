CREATE TABLE duplicate_id(
	first_id BIGINT NOT NULL,
	other_id BIGINT NOT NULL
);

-- unique extensions
INSERT INTO duplicate_id(first_id, other_id)
SELECT g.first_extension_id, e.id
FROM (
	SELECT namespace_id, UPPER(name) "name", MIN(id) first_extension_id
	FROM extension
	GROUP BY namespace_id, UPPER(name)
) g
JOIN extension e ON e.namespace_id = g.namespace_id AND UPPER(e.name) = g.name
WHERE g.first_extension_id <> e.id;

UPDATE extension e
SET download_count = e.download_count + j.other_download_count
FROM (
	SELECT d.first_id, SUM(e.download_count) other_download_count
	FROM duplicate_id d 
	JOIN extension e ON e.id = d.other_id
	GROUP BY d.first_id
) j
WHERE e.id = j.first_id;

UPDATE extension_version
SET extension_id = d.first_id
FROM duplicate_id d
WHERE extension_id = d.other_id;

UPDATE extension_review
SET extension_id = d.first_id
FROM duplicate_id d
WHERE extension_id = d.other_id;

DELETE FROM extension
WHERE id IN(SELECT other_id FROM duplicate_id);

DELETE FROM duplicate_id;

-- unique extension versions
INSERT INTO duplicate_id(first_id, other_id)
SELECT g.first_id, ev.id
FROM (
	SELECT extension_id, version, MIN(id) first_id
	FROM extension_version
	GROUP BY extension_id, version
) g
JOIN extension_version ev ON ev.extension_id = g.extension_id AND ev.version = g.version
WHERE g.first_id <> ev.id;

UPDATE extension
SET preview_id = d.first_id
FROM duplicate_id d
WHERE preview_id = d.other_id;

UPDATE extension
SET latest_id = d.first_id
FROM duplicate_id d
WHERE latest_id = d.other_id;

UPDATE file_resource
SET extension_id = d.first_id
FROM duplicate_id d
WHERE extension_id = d.other_id;

DELETE FROM extension_version
WHERE id IN(SELECT other_id FROM duplicate_id);

DELETE FROM duplicate_id;

-- unique file resources
INSERT INTO duplicate_id(first_id, other_id)
SELECT g.first_id, f.id
FROM (
	SELECT type, extension_id, name, storage_type, MIN(id) first_id
	FROM file_resource
	GROUP BY type, extension_id, name, storage_type
) g
JOIN file_resource f ON f.type = g.type AND f.extension_id = g.extension_id AND f.name = g.name AND f.storage_type = g.storage_type
WHERE g.first_id <> f.id;

UPDATE download
SET file_resource_id_not_fk = d.first_id
FROM duplicate_id d
WHERE file_resource_id_not_fk = d.other_id;

DELETE FROM file_resource
WHERE id IN(SELECT other_id FROM duplicate_id);

DROP TABLE duplicate_id;

-- add unique constraints
CREATE UNIQUE INDEX unique_extension ON extension(namespace_id, UPPER(name));
CREATE UNIQUE INDEX unique_extension_version ON extension_version(extension_id, version);

-- drop same but non-unique indices
DROP INDEX public.extension_namespace_and_name_idx;
DROP INDEX public.extension_version_ext_and_ver_idx;
