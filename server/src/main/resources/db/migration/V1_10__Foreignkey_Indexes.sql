CREATE INDEX extension__latest_id__idx ON extension (latest_id);
CREATE INDEX extension__namespace_id__idx ON extension (namespace_id);
CREATE INDEX extension__preview_id__idx ON extension (preview_id);

CREATE INDEX extension_review__extension_id__idx ON extension_review (extension_id);
CREATE INDEX extension_review__user_id__idx ON extension_review (user_id);

CREATE INDEX extension_version__extension_id__idx ON extension_version (extension_id);
CREATE INDEX extension_version__published_with_id__idx ON extension_version (published_with_id);

CREATE INDEX extension_version_bundled_extensions__extension_version_id__idx ON extension_version_bundled_extensions (extension_version_id);
CREATE INDEX extension_version_bundled_extensions__bundled_extensions_id__idx ON extension_version_bundled_extensions (bundled_extensions_id);

CREATE INDEX extension_version_dependencies__extension_version_id__idx ON extension_version_dependencies (extension_version_id);
CREATE INDEX extension_version_dependencies__dependencies_id__idx ON extension_version_dependencies (dependencies_id);

CREATE INDEX namespace_membership__namespace__idx ON namespace_membership (namespace);
CREATE INDEX namespace_membership__user_data__idx ON namespace_membership (user_data);

CREATE INDEX persisted_log__user_data__idx ON persisted_log (user_data);

-- already added by V1_7_Indexes.sql
-- CREATE INDEX extension_version_categories__extension_version_id__idx ON extension_version_categories (extension_version_id);

-- CREATE INDEX extension_version_engines__extension_version_id__idx ON extension_version_engines (extension_version_id);

-- CREATE INDEX extension_version_tags__extension_version_id__idx ON extension_version_tags (extension_version_id);

-- CREATE INDEX file_resource__extension_id__idx ON file_resource (extension_id);
