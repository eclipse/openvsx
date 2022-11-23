package org.eclipse.openvsx.repositories;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * Can be extended to run a SpringBootTest against a Postgres DB for testing
 * entities/repositories.
 * For development the test may also be configured to use an already
 * running DB, instead of always starting a new one via testcontainers.
 */
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, properties = {
                "ovsx.elasticsearch.enabled=false", //
                "spring.datasource.username=postgres", "spring.datasource.password=postgres",
                // set the following properties (e.g. un-comment the following 2 lines) to run
                // against an already running DB on a fixed port instead of starting a new one
                // every time via testcontainers:
                // "spring.datasource.driver-class-name: org.postgresql.Driver",
                // "spring.datasource.url=jdbc:postgresql://localhost:5432/postgres"
})
public abstract class PostgresDbRepositoryIntegrationTest {

}
