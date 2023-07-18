SELECT SETVAL('download_seq', (SELECT COALESCE(MAX(id), 1) FROM download)::BIGINT);
SELECT SETVAL('file_resource_seq', (SELECT COALESCE(MAX(id), 1) FROM file_resource)::BIGINT);