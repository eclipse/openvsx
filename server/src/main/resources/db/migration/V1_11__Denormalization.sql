SET SCHEMA 'public';

--
-- DEPENDENCIES
--

ALTER TABLE extension_version
    ADD dependencies character varying(2048);

UPDATE extension_version v
SET dependencies = tmp.dependencies
FROM (
    SELECT v.id, string_agg(v.fqn, ',') as dependencies FROM (
        SELECT DISTINCT v.id, n.name || '.' || dep.name as fqn
        FROM extension_version v, extension dep, extension_version_dependencies rel, namespace n
        WHERE v.id = rel.extension_version_id
        AND dep.id = rel.dependencies_id
        AND n.id = dep.namespace_id
    ) v GROUP BY v.id
) tmp
WHERE v.id = tmp.id;

DROP TABLE extension_version_dependencies;

--
-- BUNDLED EXTENSIONS
--

ALTER TABLE extension_version
    ADD bundled_extensions character varying(2048);

UPDATE extension_version v
SET bundled_extensions = tmp.bundled_extensions
FROM (
    SELECT v.id, string_agg(v.fqn, ',') as bundled_extensions FROM (
        SELECT DISTINCT v.id, n.name || '.' || dep.name as fqn
        FROM extension_version v, extension dep, extension_version_bundled_extensions rel, namespace n
        WHERE v.id = rel.extension_version_id
        AND dep.id = rel.bundled_extensions_id
        AND n.id = dep.namespace_id
    ) v GROUP BY v.id
) tmp
WHERE v.id = tmp.id;

DROP TABLE extension_version_bundled_extensions;

--
-- ENGINES
--

ALTER TABLE extension_version
    ADD engines character varying(2048);

UPDATE extension_version v
SET engines = tmp.engines
FROM (
    SELECT v.id, string_agg(v.engines, ',') as engines FROM (
        SELECT DISTINCT v.id, rel.engines
        FROM extension_version v, extension_version_engines rel
        WHERE v.id = rel.extension_version_id
    ) v GROUP BY v.id
) tmp
WHERE v.id = tmp.id;

DROP TABLE extension_version_engines;

--
-- CATEGORIES
--

ALTER TABLE extension_version
    ADD categories character varying(2048);

UPDATE extension_version v
SET categories = tmp.categories
FROM (
    SELECT v.id, string_agg(v.categories, ',') as categories FROM (
        SELECT DISTINCT v.id, rel.categories
        FROM extension_version v, extension_version_categories rel
        WHERE v.id = rel.extension_version_id
    ) v GROUP BY v.id
) tmp
WHERE v.id = tmp.id;

DROP TABLE extension_version_categories;

--
-- TAGS
--

ALTER TABLE extension_version
    ADD tags character varying(2048);

UPDATE extension_version v
SET tags = tmp.tags
FROM (
    SELECT v.id, string_agg(v.tags, ',') as tags FROM (
        SELECT DISTINCT v.id, rel.tags
        FROM extension_version v, extension_version_tags rel
        WHERE v.id = rel.extension_version_id
    ) v GROUP BY v.id
) tmp
WHERE v.id = tmp.id;

DROP TABLE extension_version_tags;
