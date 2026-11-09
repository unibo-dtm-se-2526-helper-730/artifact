# helper-dichiarazione-730

A web app that helps Italy-based investors using Revolut under the declarative tax regime (_regime dichiarativo_) calculate their substitute tax (_imposta sostitutiva_), tax loss carryforward (_zainetto fiscale_), and IVAFE (wealth tax on foreign financial assets), as well as fill out their Form 730 (_Modello 730_).

## Stack

- Java 25 / Spring Boot 4.1
- PostgreSQL 16
- React + TypeScript + Vite
- Docker
- GitHub Actions

## Project Structure

- `backend/` - API REST Spring Boot
- `frontend/` - React interface

## Project Start

TODO

## Conventions

- Commit according to [Conventional Commits](https://www.conventionalcommits.org/)

## License
Apache License 2.0 — see [LICENSE](LICENSE).


## Local Development

Prerequisites: JDK 25, Docker.

```bash
docker compose up -d db              # start PostgreSQL
cd backend
./mvnw clean package                 # compile and execute unit tests
./mvnw spring-boot:run               # starts the backend at http://localhost:8080
docker compose down -v               # (from the root) stops and cancel the database
```
```bash
./mvnw spotless:apply                # code formatter
./mvnw spotless:check                # verifies the formatting
./mvnw verify                        # coverate test and report in target/site/jacoco/index.html
./mvnw compile spotbugs:check        # static analysis
```
```bash
cd frontend
npm ci
npm run dev                          # starts the frontend at http://localhost:5173
npm run lint                         # static analysis (ESLint)
npm run format                       # code formatter (Prettier); format:check verifies it
```

## CI/CD and Releases

- `.github/workflows/check.yml` runs on every push and pull request: compilation, static checks,
  format check, tests with coverage (report uploaded as an artifact), then the tests on
  Linux, Windows and macOS with Java 25 and 26.
- `.github/workflows/deploy.yml` runs after the tests. On `main`, semantic-release computes the
  next version from the Conventional Commits since the last tag, publishes the backend and
  frontend images to GHCR, and creates the tag and the GitHub Release. On any other branch it
  only prints the version that branch would release.

Branches follow Git Flow without `develop`: `feature/*` branches are merged into a
`release/*` branch, which is merged into `main` with a merge commit (not squash), so that
semantic-release sees every `feat:` and `fix:` commit.
