# Ledger API

A Java 17 and Spring Boot 4 REST API for a small double-entry ledger. Accounts have a currency, overdraft limit, and balance. Money is stored in integer minor units (cents for EUR/USD and whole yen for JPY). Transactions are immutable collections of debit and credit entries; reversals are recorded as new transactions linked to the original.

## Current functionality

- Open and read EUR, USD, and JPY accounts.
- Record balanced transactions between accounts in the same currency.
- Enforce amount precision and overdraft limits.
- Reverse a transaction once by creating a linked transaction with its entries flipped.
- Require idempotency keys for account creation, transaction recording, and reversals.
- Return localized error messages in English or Romanian using `Accept-Language`.

## Requirements

- Java 17
- MySQL 8.x
- Docker for the Testcontainers MySQL integration test

## Configure and run

Create the database once:

```sql
CREATE DATABASE LEDGER_TRANSACTIONS;
```

Set database credentials if they differ from the defaults (`root` / `root`):

```bash
export DB_USERNAME=root
export DB_PASSWORD=your-password
```

Start the API from the project root:

```bash
./mvnw spring-boot:run
```

Flyway applies the schema migration at startup. Hibernate validates the mapped schema; it does not generate tables. The default JDBC URL is `jdbc:mysql://localhost:3306/LEDGER_TRANSACTIONS`. Override it with `DB_URL` if needed.

## API examples

All request bodies use `Content-Type: application/json`. Supply a unique `Idempotency-Key` for each logical write; reuse it for retries of the same request. Set `Accept-Language: en` or `Accept-Language: ro` to choose error-message language.

### Open an account

```http
POST /api/v1/accounts
Idempotency-Key: open-account-001
Content-Type: application/json
```

```json
{
  "currency": "EUR",
  "overdraftLimit": 100.00
}
```

### Read an account

```http
GET /api/v1/accounts/{accountId}
```

### Record a transaction

Use existing account IDs in the same currency. Debits must equal credits.

```http
POST /api/v1/transactions
Idempotency-Key: transfer-001
Content-Type: application/json
```

```json
{
  "description": "Transfer",
  "entries": [
    {
      "accountId": "11111111-1111-4111-8111-111111111111",
      "direction": "DEBIT",
      "amount": 25.50
    },
    {
      "accountId": "22222222-2222-4222-8222-222222222222",
      "direction": "CREDIT",
      "amount": 25.50
    }
  ]
}
```

### Read a transaction

```http
GET /api/v1/transactions/{transactionId}
```

### Reverse a transaction

A reversal request has no body. It creates a new transaction with each original entry flipped.

```http
POST /api/v1/transactions/{transactionId}/reversal
Idempotency-Key: reversal-001
```

## Tests

Run the Docker-free service unit tests:

```bash
./mvnw -Dtest=AccountServiceImplementationTest,TransactionServiceImplementationTest test
```

Run the MySQL integration test (requires Docker installed and running):

```bash
./mvnw -Dtest=LedgerMySqlIntegrationTest test
```

The integration test starts a disposable MySQL container and applies Flyway migrations. It covers a successful transfer and reversal, and verifies an overdraft refusal leaves account balances, transactions, and entries unchanged. This test has not been run in environments without Docker. Running all tests also includes this Docker-backed test:

```bash
./mvnw test
```

The service unit tests have been run successfully. The MySQL integration test compiles, but its runtime behavior remains unverified here because Docker is not installed. Without Docker, run the unit-test command above.

## Database and project structure

Flyway migrations are in `src/main/resources/db/migration`. Tables use the `LA_` prefix and uppercase singular names, for example `LA_ACCOUNT` and `LA_TRANSACTION`; stored amounts are integer minor units. Java packages are organized under `backend.ledger_api` into `controller`, `dto`, `entity`, `enums`, `exception`, `repository`, `service`, and `service.api`.

## Current scope and follow-up

Payment-settled message consumption and reliable transaction announcements are out of scope for this implementation. Integration coverage currently checks successful recording/reversal and that an overdraft refusal leaves no database changes. Concurrency, crash recovery, and cross-instance idempotency still need additional integration tests.
