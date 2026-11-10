# helper-730 – backend

Spring Boot REST API (Java 25) that imports broker CSV exports, stores the transactions in
PostgreSQL and computes substitute tax, zainetto, IVAFE and the Form 730 values.

```bash
docker compose up -d db          # from the repository root: PostgreSQL on localhost:5432
./mvnw spring-boot:run           # http://localhost:8080, Swagger UI at /swagger-ui.html
./mvnw verify                    # unit + integration tests (Docker needed), JaCoCo report
```

## Structure

Packages under `src/main/java/it/helper730`:

| Package | Content |
|---|---|
| `api` | REST controllers under `/api/v1`, DTOs, RFC 9457 problem details |
| `service` | Use cases and transactions: import, tax, instruments, prices |
| `ingestion` | `BrokerParser` per broker (`revolut`, `traderepublic`), CSV reader, row validation |
| `engine` | Tax engine: lots, gains and losses, zainetto, IVAFE, rule sets per year |
| `domain` | Entities and value objects, framework-free |
| `reporting` | Form 730 values (quadri T, M, W) |
| `persistence` | JPA entities, Spring Data repositories, entity mapping |
| `config` | Wiring of the framework-free classes, OpenAPI |

`domain` and `engine` depend on no framework and no other layer (`ArchitectureTest`).

Resources: `db/migration` (Flyway schema), `i18n` (Italian and English messages).

## Tests

| Kind | Where | Run by |
|---|---|---|
| Unit tests | `src/test/java`, `*Test` | `./mvnw test` (surefire) |
| Acceptance tests | `RevolutGoldenTest`, `TradeRepublicGoldenTest` on the files in `src/test/resources/golden` | `./mvnw test` |
| Integration tests | `ApiIntegrationIT`, `OpenApiSpecIT`: the whole app on PostgreSQL (Testcontainers) | `./mvnw verify` (failsafe) |

Coverage report: `target/site/jacoco/index.html`.

## Quality checks

```bash
./mvnw spotless:apply            # format (Palantir Java Format)
./mvnw spotbugs:check            # static analysis
./mvnw verify "-Dopenapi.update=true"   # regenerate docs/openapi.json after an API change
```

## Configuration

`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` (defaults: `localhost`, `5432`,
`helper730`, `helper730`, `helper730`). The Docker image is built by `Dockerfile`.