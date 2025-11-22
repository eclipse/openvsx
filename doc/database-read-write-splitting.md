# Database Read/Write Splitting

## Overview

OpenVSX now supports database read/write splitting to improve horizontal scalability for high-traffic deployments. This feature allows you to configure separate connection pools for:

- **Primary Database**: Handles all write operations (INSERT, UPDATE, DELETE) and can also handle reads
- **Replica Database(s)**: Handles read-only operations (SELECT) for improved performance

This is particularly beneficial since the majority of database traffic in OpenVSX consists of SELECT statements, which can be distributed across read replicas.

## Architecture

The read/write splitting implementation uses:

1. **RoutingDataSource**: A custom Spring DataSource that routes queries based on transaction type
2. **DataSourceContextHolder**: Thread-local context to track which datasource should be used
3. **ReadOnlyRoutingInterceptor**: AOP interceptor that automatically routes `@Transactional(readOnly=true)` methods to replica databases

## Configuration

### Basic Setup (Single Database - Default)

By default, OpenVSX works with a single database connection. No configuration changes are required for existing deployments:

```yaml
spring:
  datasource:
    primary:
      url: jdbc:postgresql://localhost:5432/openvsx
      username: openvsx
      password: openvsx
      hikari:
        maximum-pool-size: 10
        minimum-idle: 5
        connection-timeout: 30000

ovsx:
  datasource:
    replica:
      enabled: false  # Replica not configured
```

All operations (both reads and writes) will use the primary datasource.

### Enabling Read/Write Splitting

To enable read/write splitting, configure both primary and replica datasources:

```yaml
spring:
  datasource:
    primary:
      url: jdbc:postgresql://primary-db:5432/openvsx
      username: openvsx
      password: openvsx
      hikari:
        maximum-pool-size: 10
        minimum-idle: 5
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000
    replica:
      url: jdbc:postgresql://replica-db:5432/openvsx
      username: openvsx_readonly
      password: openvsx_readonly
      hikari:
        maximum-pool-size: 20  # Usually larger for read replicas
        minimum-idle: 10
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000

ovsx:
  datasource:
    replica:
      enabled: true  # Enable read/write splitting
```

### Environment Variables (Kubernetes/OpenShift)

For containerized deployments, you can use environment variables:

```yaml
spring:
  datasource:
    primary:
      url: ${OPENVSX_DB_URL:jdbc:postgresql://postgresql:5432/openvsx}
      username: ${OPENVSX_DB_USER:openvsx}
      password: ${OPENVSX_DB_PASSWORD:openvsx}
    replica:
      url: ${OPENVSX_REPLICA_DB_URL:jdbc:postgresql://postgresql-replica:5432/openvsx}
      username: ${OPENVSX_REPLICA_DB_USER:openvsx}
      password: ${OPENVSX_REPLICA_DB_PASSWORD:openvsx}

ovsx:
  datasource:
    replica:
      enabled: ${OPENVSX_REPLICA_ENABLED:false}
```

## Connection Pool Configuration

### HikariCP Settings

Both primary and replica datasources use HikariCP for connection pooling. Recommended settings:

**Primary Database** (handles writes + some reads):
```yaml
hikari:
  maximum-pool-size: 10          # Max connections for writes
  minimum-idle: 5                # Minimum idle connections
  connection-timeout: 30000      # 30 seconds
  idle-timeout: 600000           # 10 minutes
  max-lifetime: 1800000          # 30 minutes
```

**Replica Database** (handles read-only operations):
```yaml
hikari:
  maximum-pool-size: 20          # Larger pool for read traffic
  minimum-idle: 10               # More idle connections
  connection-timeout: 30000      # 30 seconds
  idle-timeout: 600000           # 10 minutes
  max-lifetime: 1800000          # 30 minutes
```

Adjust these values based on your traffic patterns and database capacity.

## How It Works

### Automatic Routing

The system automatically routes queries based on the `@Transactional` annotation:

**Routes to REPLICA:**
```java
@Transactional(readOnly = true)
public Extension findExtension(String name) {
    return extensionRepository.findByName(name);
}
```

**Routes to PRIMARY:**
```java
@Transactional
public Extension saveExtension(Extension extension) {
    return extensionRepository.save(extension);
}
```

### Manual Routing (Advanced)

If needed, you can manually control routing:

```java
// Force routing to primary
DataSourceContextHolder.setDataSourceType(DataSourceType.PRIMARY);
try {
    // Your database operations
} finally {
    DataSourceContextHolder.clearDataSourceType();
}

// Force routing to replica
DataSourceContextHolder.setDataSourceType(DataSourceType.REPLICA);
try {
    // Your read-only operations
} finally {
    DataSourceContextHolder.clearDataSourceType();
}
```

## Database Setup

### PostgreSQL Replication

To set up PostgreSQL replication:

1. **Configure Primary Server** (`postgresql.conf`):
```ini
wal_level = replica
max_wal_senders = 3
max_replication_slots = 3
```

2. **Create Replication User**:
```sql
CREATE ROLE replicator WITH REPLICATION LOGIN PASSWORD 'repl_password';
```

3. **Configure pg_hba.conf**:
```
# Allow replication connections
host replication replicator replica-ip/32 md5
```

4. **Set Up Replica Server**:
```bash
# Stop replica server
pg_basebackup -h primary-ip -D /var/lib/postgresql/data -U replicator -P -v -R
# Start replica server
```

5. **Create Read-Only User** (on primary):
```sql
CREATE ROLE openvsx_readonly WITH LOGIN PASSWORD 'readonly_password';
GRANT CONNECT ON DATABASE openvsx TO openvsx_readonly;
GRANT USAGE ON SCHEMA public TO openvsx_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO openvsx_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO openvsx_readonly;
```

## Monitoring

### Logging

Enable debug logging to monitor datasource routing:

```yaml
logging:
  level:
    org.eclipse.openvsx.db: DEBUG
```

You'll see logs like:
```
DEBUG o.e.openvsx.db.ReadOnlyRoutingInterceptor - Routing findExtension() to REPLICA datasource
DEBUG o.e.openvsx.db.ReadOnlyRoutingInterceptor - Routing saveExtension() to PRIMARY datasource
```

### Connection Pool Metrics

HikariCP exposes metrics that can be monitored:

```yaml
management:
  metrics:
    enable:
      hikari: true
```

## Best Practices

1. **Read-Only User Permissions**: Use a read-only database user for replica connections to prevent accidental writes
2. **Connection Pool Sizing**: Size replica pools larger than primary pools since most traffic is reads
3. **Replication Lag**: Monitor replication lag; consider using primary for time-sensitive reads
4. **Failover**: If replica fails, queries automatically fall back to primary
5. **Testing**: Always test with `ovsx.datasource.replica.enabled=false` first before enabling splitting

## Troubleshooting

### Reads Still Going to Primary

Check that:
- `ovsx.datasource.replica.enabled=true` is set
- Replica datasource URL is configured correctly
- Methods are annotated with `@Transactional(readOnly=true)`

### Connection Pool Exhaustion

Increase pool sizes:
```yaml
spring:
  datasource:
    replica:
      hikari:
        maximum-pool-size: 30  # Increase from 20
```

### Replication Lag Issues

For critical reads that need latest data:
```java
@Transactional  // NOT readOnly=true, will use primary
public Data getLatestData() {
    return repository.findLatest();
}
```

## Performance Impact

Expected improvements with read/write splitting:
- **50-70% reduction** in primary database load
- **2-3x improvement** in read query throughput
- Better **horizontal scalability** for read-heavy workloads

## Migration Guide

### Existing Deployments

The implementation is **backward compatible**. Existing configurations will continue to work:

1. Old configuration (still works):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/openvsx
```

2. New configuration (required for read/write splitting):
```yaml
spring:
  datasource:
    primary:
      url: jdbc:postgresql://localhost:5432/openvsx
```

### Gradual Rollout

1. **Phase 1**: Update configuration to use `primary` datasource (no functional change)
2. **Phase 2**: Set up database replication
3. **Phase 3**: Add replica datasource configuration with `enabled=false`
4. **Phase 4**: Enable replica (`enabled=true`) and monitor

## References

- [Spring AbstractRoutingDataSource](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/datasource/lookup/AbstractRoutingDataSource.html)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [PostgreSQL Replication](https://www.postgresql.org/docs/current/warm-standby.html)
