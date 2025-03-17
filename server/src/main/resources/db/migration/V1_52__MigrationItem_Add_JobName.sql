ALTER TABLE migration_item ADD COLUMN job_name CHARACTER VARYING(255);

UPDATE migration_item
SET job_name = 'SetPreReleaseMigration'
WHERE migration_script = 'V1_26__Extension_Set_PreRelease.sql';

UPDATE migration_item
SET job_name = 'RenameDownloadsMigration'
WHERE migration_script = 'V1_28__MigrationItem.sql';

UPDATE migration_item
SET job_name = 'ExtractVsixManifestMigration'
WHERE migration_script = 'V1_32__FileResource_Extract_VsixManifest.sql';

UPDATE migration_item
SET job_name = 'FixTargetPlatformMigration'
WHERE migration_script = 'V1_34__ExtensionVersion_Fix_TargetPlatform.sql';

UPDATE migration_item
SET job_name = 'GenerateSha256ChecksumMigration'
WHERE migration_script = 'V1_35__FileResource_Generate_Sha256_Checksum.sql';

UPDATE migration_item
SET job_name = 'CheckPotentiallyMaliciousExtensionVersions'
WHERE migration_script = 'V1_46__ExtensionVersion_PotentiallyMalicious.sql';

UPDATE migration_item
SET job_name = 'LocalNamespaceLogoMigration'
WHERE migration_script = 'V1_48__Local_Storage_Namespace.sql';

UPDATE migration_item
SET job_name = 'LocalFileResourceContentMigration'
WHERE migration_script = 'V1_48__Local_Storage_FileResource.sql';

UPDATE migration_item
SET job_name = 'RemoveFileResourceTypeResourceMigration'
WHERE migration_script = 'V1_50__FileResource_Remove_Resource.sql';

ALTER TABLE migration_item DROP COLUMN migration_script;
ALTER TABLE migration_item ALTER COLUMN job_name SET NOT NULL;

-- reschedule file resources that haven't been deleted yet
UPDATE migration_item mi
SET migration_scheduled = FALSE
FROM file_resource fr
WHERE mi.entity_id = fr.id
AND mi.job_name = 'RemoveFileResourceTypeResourceMigration';