# Game Wallet Service

This project is a wallet service designed for high-traffic gaming or loyalty platforms. It manages virtual credits such as Gold Coins or Diamonds in a closed-loop system with strong consistency guarantees. The service is built using Spring Boot and PostgreSQL and follows a ledger-based architecture to ensure auditability and correctness under concurrent load.

## Live Application

The application is deployed and accessible at https://wallet19.onrender.com/

API documentation is available at https://wallet19.onrender.com/swagger-ui.html

Health checks can be accessed at https://wallet-service.render.com/actuator/health


## Test Users for Live Application

The live application at https://wallet19.onrender.com/ comes with pre-configured test users that you can use to try the APIs.

Available test users:
- user1: Starting balances are 1000 GOLD, 10 DIAMOND, and 100 POINTS
- user2: Starting balances are 500 GOLD, 5 DIAMOND, and 50 POINTS

Available asset types:
- GOLD: Unlimited supply asset
- DIAMOND: Limited supply asset with 1000000 units in SYSTEM wallet
- POINTS: Unlimited supply asset

Important: User identifiers and asset codes are case-sensitive. Always use lowercase for user identifiers like user1 and uppercase for asset codes like GOLD. Using incorrect casing will result in not-found errors.

## Running the Application Locally

### Prerequisites

Ensure the following are installed on your machine:
- Java 21 or higher
- Maven 3.8 or higher
- PostgreSQL 14 or higher (if running without Docker)
- Docker and Docker Compose (if using containerized setup)

### Pull from GitHub

Clone the repository from GitHub and navigate to the project directory.

```
git clone https://github.com/DevendraSinghNegi17/Wallet19.git
cd Wallet19
```

### Option 1: Running with Docker Compose

This is the recommended approach as it requires no manual database setup. Docker Compose will start both the application and PostgreSQL automatically.

Start the containers using the following command:

```
docker-compose up --build
FOR RUNNING IN DETACHED MODE: docker-compose up -d
```

Docker Compose will pull the PostgreSQL image, create the databascleae container, build the Spring Boot application, and start both services. The application will be available on port 8080.

On first startup, the database schema is created automatically by Hibernate. The data.sql script in src/main/resources runs immediately after to insert initial asset types and wallet data.

To stop the containers, press Ctrl+C or run:

```
docker-compose down
```

To completely remove volumes and start fresh, use:

```
docker-compose down -v
```

### Option 2: Running Locally Without Docker

If you prefer to run the application directly on your machine, follow these steps.

First, ensure PostgreSQL is running locally on port 5432. Create a database named wallet_db and a user with appropriate permissions:

```
CREATE DATABASE db-name;
CREATE USER user-name WITH PASSWORD 'user-password';
GRANT ALL PRIVILEGES ON DATABASE db-name TO user-name;
```

Update the database credentials in src/main/resources/application-local.yml if they differ from the defaults.

Build the application using Maven:

```
mvn clean install
```

Run the application:

```
mvn spring-boot:run
```

The application will start on port 8080. The schema and seed data will be created automatically on startup.

### Verifying the Application

Once the application is running, verify it is working correctly by accessing the health check endpoint:

```
http://localhost:8080/actuator/health
```

You should receive a response indicating the application status is UP.

Access the Swagger UI to explore the API documentation:

```
http://localhost:8080/swagger-ui.html
```

The application is now ready to accept requests.

## Database Schema

The wallet service uses four main tables to ensure data integrity and auditability.

### Asset Table

Defines the types of virtual currencies available in the system. Each asset has a code and a flag indicating whether it has a limited supply. If limited_supply is true, assets are transferred from a system wallet. If false, they are created on demand.

### Wallet Table

Stores user balances. Each wallet is identified by a combination of user_id and asset. The balance field tracks the current amount and cannot be negative. The version field is used for optimistic locking to handle concurrent updates safely. Timestamps track when the wallet was created and last updated.

### Ledger Entry Table

An immutable audit log of all transactions. Every wallet operation creates a ledger entry with debit_user, credit_user, asset, amount, and a reference. The idempotency_key field ensures that duplicate requests are not processed twice. This table provides a complete history of all fund movements.

### Idempotency Key Table

Stores unique keys provided by clients to prevent duplicate transaction processing. When a request is received, the key is inserted into this table. If the same key is sent again, the database rejects it due to the unique constraint, preventing double-processing.

## API Endpoints

### Top-up Wallet

Adds funds to a user wallet. For limited-supply assets, the amount is debited from the SYSTEM wallet and credited to the user. For unlimited-supply assets, the amount is simply added to the user wallet.

Request body includes userId, asset code, amount, and idempotencyKey.

Response indicates success or failure with an appropriate message.

### Credit Bonus

Credits a promotional or reward amount to a user wallet. Similar to top-up but includes a reason field to describe why the bonus was given, such as daily login reward or referral bonus.

Request body includes userId, asset code, amount, idempotencyKey, and reason.

Response confirms the bonus was credited successfully.

### Spend Funds

Deducts funds from a user wallet for a purchase or transaction. The amount is debited from the user and credited to the SYSTEM wallet for limited-supply assets. For unlimited-supply assets, the amount is simply removed from the user wallet.

Request body includes userId, asset code, amount, idempotencyKey, and orderId to link the transaction to an external order.

Response confirms the spend was successful.

### Get Balance

Retrieves the current balance for a specific user and asset combination. This endpoint does not modify any data.

Request parameters include userId and asset code.

Response includes userId, asset, balance, and timestamps in a BalanceResponse object.

### Get Transactions

Retrieves the transaction history for a specific user. Returns a list of all ledger entries where the user appears as either the debit or credit party.

Request parameter includes userId.

Response includes a list of TransactionResponse objects, each containing transaction id, debit user, credit user, asset, amount, reference, and timestamp.

### Ledger Replay

An admin endpoint that reconstructs all wallet balances from the ledger. This is useful for auditing or recovering from data corruption. It resets all wallets to zero and replays every ledger entry in order to recalculate balances.

No request body is needed.

Response confirms the replay was completed.

## Technology Choices

Spring Boot was chosen as the backend framework because it provides robust support for transactional data access, dependency injection, and production-grade configuration management. Spring Data JPA is used to simplify persistence while still allowing fine-grained control over transactions and locking behavior.

PostgreSQL was selected as the database because it offers strong ACID guarantees, row-level locking, and mature support for concurrent transactional workloads. These properties are essential for a wallet system where data integrity is critical.

Docker and Docker Compose are used to ensure consistent local development and deployment environments. By containerizing both the application and the database, the system can be run identically across different machines without manual setup.

The wallet uses a ledger-based design rather than directly mutating balances without history. Every credit or debit operation is recorded as an immutable ledger entry, allowing full auditability and the ability to reconstruct balances if required.

## Concurrency Handling

Concurrency is handled using pessimistic locking at the database level. When a wallet operation begins, the service acquires a row-level lock on the affected wallet records using SELECT FOR UPDATE. This prevents other transactions from modifying the same wallet simultaneously, ensuring that balance updates are serialized and consistent.

This approach avoids race conditions where two concurrent operations could read the same balance, apply changes independently, and overwrite each other updates. PostgreSQL ensures row-level isolation, so concurrent updates to different wallets do not block each other.

In addition, idempotency keys are used for all state-changing operations. If a client retries a request due to network issues or timeouts, the system detects duplicate requests and ensures that the transaction is processed only once. The idempotency key is stored in a separate table with a unique constraint, so duplicate keys are rejected at the database level.

Together, pessimistic locking, idempotency, and transactional boundaries ensure that balances never go negative, transactions are never lost, and the system remains consistent even under heavy concurrent load.

## Testing

The project includes comprehensive test coverage to ensure correctness under various scenarios. Test cases were written with assistance from ChatGPT to cover edge cases and concurrent scenarios.

Unit tests verify the service layer in isolation using mocked repositories. These tests cover successful operations for top-up, bonus, and spend with both limited and unlimited supply assets. They also verify error handling for duplicate idempotency keys, insufficient balance, missing assets, and missing wallets. One test confirms that new wallets are automatically created when needed.

Integration tests run the full application stack with an in-memory H2 database. These tests simulate real concurrent scenarios, such as ten threads simultaneously depositing funds into the same wallet. The tests verify that the final balance is correct and no transactions are lost. Other integration tests cover concurrent spending, duplicate key rejection across threads, and ledger entry recording.

Controller tests verify the REST API endpoints using MockMvc. These tests ensure that valid requests return 200 OK responses, invalid inputs are rejected with appropriate error codes, and duplicate requests return 409 Conflict responses.

All tests can be run using mvn clean test.

## Error Handling

The API returns standardized error responses for all failure scenarios. Duplicate requests return a 409 Conflict status with code DUPLICATE_REQUEST. Insufficient balance errors return a 400 Bad Request status with code INSUFFICIENT_BALANCE. Missing assets or wallets return a 404 Not Found status. Validation errors for invalid input return a 400 Bad Request status with code VALIDATION_ERROR.

Each error response includes a success flag set to false, an error code, and a descriptive message.



## Project Structure

The source code is organized into controllers for REST endpoints, DTOs for request and response objects, exceptions for custom error handling, models for JPA entities, repositories for data access, and services for business logic. Configuration files and database scripts are located in the resources directory. Test classes mirror the main source structure.

## Future Work

Possible enhancements include adding wallet-to-wallet transfers, implementing transaction reversal and refund capabilities, adding Redis caching for read-heavy operations, implementing rate limiting per user, supporting multi-currency wallets, creating an admin dashboard for monitoring, and adding event sourcing for a complete audit trail.
