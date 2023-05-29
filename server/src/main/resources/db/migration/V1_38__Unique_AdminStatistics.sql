CREATE TEMP TABLE tmp_admin_statistics AS
SELECT year, month, MIN(id) id
FROM admin_statistics
GROUP BY year, month;

DELETE FROM admin_statistics_extensions_by_rating
WHERE admin_statistics_id NOT IN(SELECT id FROM tmp_admin_statistics);

DELETE FROM admin_statistics_publishers_by_extensions_published
WHERE admin_statistics_id NOT IN(SELECT id FROM tmp_admin_statistics);

DELETE FROM admin_statistics_top_most_active_publishing_users
WHERE admin_statistics_id NOT IN(SELECT id FROM tmp_admin_statistics);

DELETE FROM admin_statistics_top_most_downloaded_extensions
WHERE admin_statistics_id NOT IN(SELECT id FROM tmp_admin_statistics);

DELETE FROM admin_statistics_top_namespace_extension_versions
WHERE admin_statistics_id NOT IN(SELECT id FROM tmp_admin_statistics);

DELETE FROM admin_statistics_top_namespace_extensions
WHERE admin_statistics_id NOT IN(SELECT id FROM tmp_admin_statistics);

DELETE FROM admin_statistics
WHERE id NOT IN(SELECT id FROM tmp_admin_statistics);

DROP TABLE tmp_admin_statistics;

CREATE UNIQUE INDEX unique_admin_statistics ON admin_statistics(year, month);
