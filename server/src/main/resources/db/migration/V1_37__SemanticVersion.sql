-- add semver columns
ALTER TABLE extension_version ADD COLUMN semver_major INT;
ALTER TABLE extension_version ADD COLUMN semver_minor INT;
ALTER TABLE extension_version ADD COLUMN semver_patch INT;
ALTER TABLE extension_version ADD COLUMN semver_pre_release VARCHAR;
ALTER TABLE extension_version ADD COLUMN semver_is_pre_release BOOLEAN;
ALTER TABLE extension_version ADD COLUMN semver_build_metadata VARCHAR;

-- fill semver columns
UPDATE extension_version SET
    semver_major = semver.matches[1]::INT,
    semver_minor = semver.matches[2]::INT,
    semver_patch = semver.matches[3]::INT,
    semver_pre_release = semver.matches[5],
    semver_is_pre_release = semver.matches[5] IS NOT NULL,
    semver_build_metadata = semver.matches[10]
FROM(
    SELECT id, regexp_match(version, '^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-((0|[1-9][0-9]*|[0-9]*[a-zA-Z-][0-9a-zA-Z-]*)(\.(0|[1-9][0-9]*|[0-9]*[a-zA-Z-][0-9a-zA-Z-]*))*))?(\+([0-9a-zA-Z-]+(\.[0-9a-zA-Z-]+)*))?$') matches
    FROM extension_version
) semver
WHERE extension_version.id = semver.id;

-- add universal target platform column
ALTER TABLE extension_version ADD COLUMN universal_target_platform BOOLEAN;

UPDATE extension_version
SET universal_target_platform = TRUE
WHERE target_platform = 'universal';

UPDATE extension_version
SET universal_target_platform = FALSE
WHERE universal_target_platform IS NULL;

-- create sorting indices
CREATE INDEX extension_version_order_by_idx ON extension_version USING btree(
    semver_major DESC,
    semver_minor DESC,
    semver_patch DESC,
    semver_is_pre_release ASC,
    universal_target_platform DESC,
    target_platform ASC,
    timestamp DESC
);

CREATE INDEX extension_version_latest_order_by_idx ON extension_version USING btree(
    extension_id ASC,
    semver_major DESC,
    semver_minor DESC,
    semver_patch DESC,
    semver_is_pre_release ASC,
    universal_target_platform DESC,
    target_platform ASC,
    timestamp DESC
);

CREATE INDEX extension_version_version_map_order_by_idx ON extension_version USING btree(
    extension_id ASC,
    semver_major DESC,
    semver_minor DESC,
    semver_patch DESC,
    semver_is_pre_release ASC,
    version ASC
);

CREATE INDEX extension_version_version_list_order_by_idx ON extension_version USING btree(
    semver_major DESC,
    semver_minor DESC,
    semver_patch DESC,
    semver_is_pre_release ASC,
    version ASC
);

CREATE INDEX extension_version_by_target_platform_order_by_idx ON extension_version USING btree(
    extension_id ASC,
    universal_target_platform DESC,
    target_platform ASC,
    semver_major DESC,
    semver_minor DESC,
    semver_patch DESC,
    semver_is_pre_release ASC,
    timestamp DESC
);