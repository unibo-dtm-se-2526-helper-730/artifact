# helper-730

A web app that helps Italy-based investors using foreign brokers under the declarative tax
regime (_regime dichiarativo_) compute their substitute tax (_imposta sostitutiva_), tax loss
carryforward (_zainetto fiscale_) and IVAFE, and fill in the investment sections of the
Form 730 (_Modello 730_).

The user uploads the CSV exported by the broker; the app recognises the format, imports the
transactions and does the rest. Brokers are pluggable:

| Broker | File to upload |
|---|---|
| Revolut | trading account statement (CSV) |
| Trade Republic | transaction export (CSV) |

The broker also determines the country written in quadro W (Lithuania for Revolut, Germany
for Trade Republic).

For Trade Republic the app reads the columns of the official export, with the `type` column
holding the operation types of the app (`BUY`, `SELL`, `DIVIDEND`, `COUPON`, `CASH_TOP_UP`,
`CASH_WITHDRAWAL`). Trade Republic identifies securities by ISIN, so in the app their ticker
is the ISIN: the same ETF bought at both brokers appears as two instruments.

Adding a broker means writing a `BrokerParser` in `ingestion` (header, row mapping, country)
and registering it in `CoreConfig`: the tax engine works on broker-independent transactions
and does not change. Sample files are in `backend/src/test/resources/golden/`.

## Architecture

Three containers: a React single-page app served by nginx, a Spring Boot REST API and a
PostgreSQL database. nginx (or the Vite dev server) forwards `/api` to the backend, so the
browser always talks to a single origin.

```
browser ──► frontend (React, nginx) ──/api──► backend (Spring Boot) ──► PostgreSQL
```

The backend is a layered application around a framework-free core:

| Package | Role |
|---|---|
| `api` | REST controllers, DTOs, error handling (RFC 9457 problem details) |
| `service` | Use cases and transactions: orchestrates ingestion, engine and persistence |
| `ingestion` | Broker CSV parsers: one `BrokerParser` per broker, chosen from the header by `BrokerParserRegistry` |
| `engine` | Tax calculation: lots, capital gains, zainetto, IVAFE, yearly rule sets |
| `domain` | Entities and value objects (`Transaction`, `Instrument`, `Lot`, `Money`, …) |
| `persistence` | JPA entities, Spring Data repositories, Flyway migrations |
| `reporting` | Form 730 declaration items built from the tax result |

`domain` and `engine` depend on no framework and on no other layer: an ArchUnit test
(`ArchitectureTest`) enforces it.

The Web API is versioned under `/api/v1`. Its OpenAPI specification is committed in
[`docs/openapi.json`](docs/openapi.json). A test (`OpenApiSpecIT`) fails the build when the
API and the file differ. Swagger UI is at <http://localhost:8080/swagger-ui.html>.

## Tools

| | Backend | Frontend |
|---|---|---|
| Language | Java 25, Spring Boot 4.1 | TypeScript, React 19, Vite |
| Build and dependencies | Maven (wrapper `./mvnw`, `pom.xml`) | npm (`package.json`, `package-lock.json`) |
| Tests | JUnit 5, Testcontainers, ArchUnit | Vitest, Testing Library |
| Coverage | JaCoCo | Vitest (v8) |
| Static analysis | SpotBugs | ESLint, `tsc` |
| Formatting | Spotless (Palantir Java Format) | Prettier |

Database: PostgreSQL 16, schema managed by Flyway. Releases: semantic-release (root
`package.json`, release tooling only).

## Running locally

Prerequisites: JDK 25, Node 26, Docker.

```bash
docker compose up -d db          # PostgreSQL on localhost:5432

cd backend
./mvnw spring-boot:run           # API on http://localhost:8080

cd frontend
npm ci
npm run dev                      # app on http://localhost:5173
```

Checks, from `backend/` and `frontend/`:

```bash
./mvnw verify                    # unit + integration tests, coverage in target/site/jacoco
./mvnw spotbugs:check spotless:check
./mvnw verify "-Dopenapi.update=true"   # regenerate docs/openapi.json after an API change

npm test                         # unit and component tests
npm run coverage                 # coverage in coverage/index.html
npm run typecheck && npm run lint && npm run format:check
```

`docker compose down -v` stops the database and deletes its data.

## Running with Docker

Builds and starts database, backend and frontend:

```bash
docker compose up --build        # app on http://localhost:3000, API on http://localhost:8080
```

Database name, user and password default to `helper730`; override them with `DB_NAME`,
`DB_USER` and `DB_PASSWORD` (environment or a `.env` file).

Released images are on GitHub Container Registry:

```bash
docker pull ghcr.io/<owner>/helper-730-backend:<version>
docker pull ghcr.io/<owner>/helper-730-frontend:<version>
```

The backend reads `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`; the frontend
image expects the backend at `http://backend:8080` (see `frontend/nginx.conf`).

## Git workflow

- `main` is always releasable and only changes through pull requests.
- Every change is a short-lived branch from `main`, named after its type: `feat/…`, `fix/…`,
  `test/…`, `ci/…`, `build/…`, `docs/…`, `refactor/…`.
- Pull requests are merged with **squash merge**: each one becomes a single commit on `main`.
- Commits follow [Conventional Commits](https://www.conventionalcommits.org/). With squash
  merge the **pull request title** becomes the commit on `main`, so it must be conventional
  too, e.g. `feat(api): add CSV export`, `fix(ivafe): prorate by days held`.

## CI/CD and releases

- [`check.yml`](.github/workflows/check.yml) runs on every push and pull request: compilation,
  static analysis, format checks, backend and frontend tests with coverage (reports uploaded
  as artifacts), Docker build, then the backend tests on Linux, Windows and macOS with Java 25
  and 26.
- [`deploy.yml`](.github/workflows/deploy.yml) runs after the tests, never on pull requests.
  On `main`, semantic-release reads the commits since the last tag and computes the next
  [SemVer](https://semver.org/) version:

  | Commit | Release |
  |---|---|
  | `fix:`, `perf:` | patch (0.2.0 → 0.2.1) |
  | `feat:` | minor (0.2.0 → 0.3.0) |
  | `feat!:` or `BREAKING CHANGE:` | major (0.2.0 → 1.0.0) |
  | `docs:`, `test:`, `ci:`, `build:`, `refactor:`, `chore:` | none |

  It then publishes both images to GHCR (tags `X.Y.Z`, `X.Y` and `latest`), creates the
  `vX.Y.Z` tag and the GitHub Release with generated notes. On other branches it only prints
  the version it would release.

The version lives in the git tag only: `pom.xml` and `package.json` keep a placeholder,
and the release writes the real version into the build without committing it.

## License

Apache License 2.0, see [LICENSE](LICENSE).