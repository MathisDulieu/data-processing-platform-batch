# data-processing-platform-batch

Batch processing component of the Data Processing Platform. Reads uploaded files from PostgreSQL, parses CSV and JSON content, validates and transforms records, then persists the results. Triggered via REST endpoint by the API component.

## Requirements

- Java 21
- Maven 3.9+
- PostgreSQL (via [data-processing-docker-dev-env](../data-processing-docker-dev-env))

## Profiles

| Profile | Usage |
|---|---|
| `dev` | Local development — connects to local PostgreSQL |
| `prod` | Railway deployment — reads credentials from environment variables |

## Run locally

```bash
mvn spring-boot:run -Pdev
```

The batch starts on port `8081` and waits for incoming requests.

## Run tests

Start the test database first:

```bash
docker compose -f docker-compose.test.yml up -d
```

Then run the tests:

```bash
mvn test
```

Or use the "Run all tests" shortcut in IntelliJ directly.

## Trigger a batch run

```http
POST http://localhost:8081/batch/run
```

## Build for production

```bash
mvn package -Pprod -DskipTests
```

## Environment variables

| Variable | Description |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
