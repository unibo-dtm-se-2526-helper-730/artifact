# helper-dichiarazione-730

A web app that helps Italy-based investors using Revolut under the declarative tax regime (_regime dichiarativo_) calculate their substitute tax (_imposta sostitutiva_), tax loss carryforward (_zainetto fiscale_), and IVAFE (wealth tax on foreign financial assets), as well as fill out their Form 730 (_Modello 730_).

## Stack

- Java 21 / Spring Boot 4.1
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

Prerequisites: JDK 21, Docker.

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
```
