-- Time-series download analytics schema. Requires a PostgreSQL image with the timescaledb
-- extension available. See V1_72__Download_Analytics.sql.conf: continuous aggregates cannot
-- be created inside a transaction.

CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE download_event (
    time TIMESTAMPTZ NOT NULL,
    extension_id BIGINT NOT NULL,
    extension_version_id BIGINT NOT NULL,
    namespace VARCHAR(255) NOT NULL,
    extension_name VARCHAR(255) NOT NULL,
    version VARCHAR(255) NOT NULL,
    target_platform VARCHAR(255) NOT NULL,
    country CHAR(2),
    ip VARCHAR(45),
    user_agent TEXT,
    count INTEGER NOT NULL DEFAULT 1
);

SELECT create_hypertable('download_event', by_range('time', INTERVAL '7 days'));

CREATE INDEX de_ext_time ON download_event (extension_id, time DESC);

-- materialized_only = false keeps the not-yet-materialized tail (e.g. today) queryable
-- through real-time aggregation, which the settling-margin logic in the query service
-- relies on.
CREATE MATERIALIZED VIEW download_stats_daily
WITH (timescaledb.continuous, timescaledb.materialized_only = false) AS
SELECT time_bucket('1 day', time) AS day,
       extension_id, extension_version_id, version, target_platform, country,
       SUM(count) AS downloads
FROM download_event
GROUP BY time_bucket('1 day', time), extension_id, extension_version_id, version, target_platform, country
WITH NO DATA;

SELECT add_continuous_aggregate_policy('download_stats_daily',
    start_offset => INTERVAL '3 days',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');

-- compress raw chunks after 7 days, drop them after 90 days; the daily aggregate is kept forever
ALTER TABLE download_event SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'extension_id',
    timescaledb.compress_orderby = 'time DESC'
);

SELECT add_compression_policy('download_event', INTERVAL '7 days');

SELECT add_retention_policy('download_event', INTERVAL '90 days');
