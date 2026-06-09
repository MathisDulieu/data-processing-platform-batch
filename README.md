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

Then run the tests:

```bash
mvn test
```

Or use the "Run all tests" shortcut in IntelliJ directly.

## Endpoints

### POST /batch/run

Triggers a batch run. Reads all pending files from the database, processes them, and returns the number of processed files.

```json
{
  "status": "PROCESSED",
  "processedFiles": 3
}
```

### GET /batch/status

Returns the status of the latest batch run.

```json
{
  "status": "PROCESSED",
  "totalRecords": 10,
  "validRecords": 8,
  "rejectedRecords": 2,
  "startedAt": "2025-01-15T10:30:00",
  "finishedAt": "2025-01-15T10:30:05",
  "message": null
}
```

## Sample files

Example files showing the expected format are available in the [`samples`](./samples) folder:

- [`samples/sample.csv`](./samples/sample.csv) — CSV format
- [`samples/sample.json`](./samples/sample.json) — JSON format

## HTTP requests

Ready-to-use HTTP request files for IntelliJ are available in the [`http`](./http) folder:

- [`http/batch-run.http`](./http/batch-run.http) — Trigger a batch run
- [`http/batch-status.http`](./http/batch-status.http) — Get the latest batch status

## Environment variables

| Variable | Description |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
