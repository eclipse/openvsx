# Quick Start: Database Read/Write Splitting

## TL;DR

OpenVSX now supports routing read queries to replica databases for better scalability.

## For Single Database (Default - No Changes Needed)

Your existing config still works:
```yaml
spring:
  datasource:
    primary:  # Changed from 'url' to 'primary.url'
      url: jdbc:postgresql://localhost:5432/openvsx
      username: openvsx
      password: openvsx
```

## To Enable Read/Write Splitting

1. **Set up PostgreSQL replication** (primary → replica)

2. **Update application.yml**:
```yaml
spring:
  datasource:
    primary:
      url: jdbc:postgresql://primary:5432/openvsx
      username: openvsx
      password: openvsx
      hikari:
        maximum-pool-size: 10
    replica:
      url: jdbc:postgresql://replica:5432/openvsx
      username: openvsx_readonly  # Use read-only user
      password: readonly_password
      hikari:
        maximum-pool-size: 20  # Larger for read traffic

ovsx:
  datasource:
    replica:
      enabled: true  # Enable routing
```

3. **Done!** No code changes required.

## How It Works

Automatically routes based on `@Transactional` annotation:

```java
// Goes to REPLICA (read-only)
@Transactional(readOnly = true)
public Extension findExtension(String name) {
    return repository.findByName(name);
}

// Goes to PRIMARY (write)
@Transactional
public Extension saveExtension(Extension extension) {
    return repository.save(extension);
}
```

## Verification

Enable logging to see routing:
```yaml
logging:
  level:
    org.eclipse.openvsx.db: DEBUG
```

## Common Issues

**Q: Reads still going to primary?**
- Check `ovsx.datasource.replica.enabled=true`
- Verify replica URL is correct
- Ensure methods use `@Transactional(readOnly=true)`

**Q: Connection pool exhausted?**
- Increase `hikari.maximum-pool-size` for replica

**Q: Need latest data (replication lag)?**
- Use `@Transactional` (not `readOnly=true`) to read from primary

## Full Documentation

See [database-read-write-splitting.md](database-read-write-splitting.md) for complete setup guide.
