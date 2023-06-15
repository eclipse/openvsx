CREATE SEQUENCE admin_statistics_seq INCREMENT 50 OWNED BY public.admin_statistics.id;
SELECT SETVAL('admin_statistics_seq', (SELECT COALESCE(MAX(id), 1) FROM admin_statistics)::BIGINT);

CREATE SEQUENCE azure_download_count_processed_item_seq INCREMENT 50 OWNED BY public.azure_download_count_processed_item.id;
SELECT SETVAL('azure_download_count_processed_item_seq', (SELECT COALESCE(MAX(id), 1) FROM azure_download_count_processed_item)::BIGINT);

ALTER SEQUENCE download_id_seq RENAME TO download_seq;
ALTER SEQUENCE download_seq INCREMENT 50;

CREATE SEQUENCE extension_seq INCREMENT 50 OWNED BY public.extension.id;
SELECT SETVAL('extension_seq', (SELECT COALESCE(MAX(id), 1) FROM extension)::BIGINT);

CREATE SEQUENCE extension_review_seq INCREMENT 50 OWNED BY public.extension_review.id;
SELECT SETVAL('extension_review_seq', (SELECT COALESCE(MAX(id), 1) FROM extension_review)::BIGINT);

CREATE SEQUENCE extension_version_seq INCREMENT 50 OWNED BY public.extension_version.id;
SELECT SETVAL('extension_version_seq', (SELECT COALESCE(MAX(id), 1) FROM extension_version)::BIGINT);

ALTER SEQUENCE file_resource_id_seq RENAME TO file_resource_seq;
ALTER SEQUENCE file_resource_seq INCREMENT 50;

CREATE SEQUENCE migration_item_seq INCREMENT 50 OWNED BY public.migration_item.id;
SELECT SETVAL('migration_item_seq', (SELECT COALESCE(MAX(id), 1) FROM migration_item)::BIGINT);

CREATE SEQUENCE namespace_seq INCREMENT 50 OWNED BY public.namespace.id;
SELECT SETVAL('namespace_seq', (SELECT COALESCE(MAX(id), 1) FROM namespace)::BIGINT);

CREATE SEQUENCE namespace_membership_seq INCREMENT 50 OWNED BY public.namespace_membership.id;
SELECT SETVAL('namespace_membership_seq', (SELECT COALESCE(MAX(id), 1) FROM namespace_membership)::BIGINT);

CREATE SEQUENCE persisted_log_seq INCREMENT 50 OWNED BY public.persisted_log.id;
SELECT SETVAL('persisted_log_seq', (SELECT COALESCE(MAX(id), 1) FROM persisted_log)::BIGINT);

CREATE SEQUENCE personal_access_token_seq INCREMENT 50 OWNED BY public.personal_access_token.id;
SELECT SETVAL('personal_access_token_seq', (SELECT COALESCE(MAX(id), 1) FROM personal_access_token)::BIGINT);

CREATE SEQUENCE signature_key_pair_seq INCREMENT 50 OWNED BY public.signature_key_pair.id;
SELECT SETVAL('signature_key_pair_seq', (SELECT COALESCE(MAX(id), 1) FROM signature_key_pair)::BIGINT);

CREATE SEQUENCE user_data_seq INCREMENT 50 OWNED BY public.user_data.id;
SELECT SETVAL('user_data_seq', (SELECT COALESCE(MAX(id), 1) FROM user_data)::BIGINT);