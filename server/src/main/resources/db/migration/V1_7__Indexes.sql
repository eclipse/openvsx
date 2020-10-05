CREATE INDEX extension_namespace_and_name_idx ON extension (namespace_id, upper(name));

CREATE INDEX extension_version_ext_and_ver_idx ON extension_version (extension_id, version);

CREATE INDEX file_resource_extension_idx ON file_resource (extension_id);

CREATE INDEX engines_extension_version_idx ON extension_version_engines (extension_version_id);

CREATE INDEX categories_extension_version_idx ON extension_version_categories (extension_version_id);

CREATE INDEX tags_extension_version_idx ON extension_version_tags (extension_version_id);
