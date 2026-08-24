-- Clears the backlog of already-completed migration_item rows that accumulated before automatic
-- cleanup existed (see MigrationItemCleanupFilter). This is the last such cleanup migration ever
-- needed: from now on, completed items are deleted as their jobs finish instead of piling up here.
DELETE FROM migration_item WHERE migration_scheduled = true;
