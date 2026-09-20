# Moneta Angular frontend

Angular dashboard for the Spring Boot banking API. It supports registration, login, account overview and creation, transaction history, deposit, withdrawal, transfer, and profile changes.

## Run locally

1. Ensure your existing MySQL container exposes `banking_system` on port `3306` and Redis exposes port `6379`, as configured in `application-dev.properties` and `application.properties`. The dev profile disables Spring Boot's automatic Docker Compose connection overrides.
2. Start Spring Boot from the repository root: `./mvnw spring-boot:run` (or `mvn spring-boot:run`). The API listens on `http://localhost:8081`.
3. In this directory run `npm install` and `npm start`.
4. Open `http://localhost:4200`.

The Angular dev server proxies `/api` to `http://localhost:8081`, so no browser CORS configuration is needed for local development. The frontend stores the JWT in session storage, which ends the browser session on closing the tab. Changing the account email requires signing in again because the JWT identifies the old email.

The repository also includes an optional `docker-compose.yml` for a separate MySQL/Redis pair on host ports `3307` and `6380`. You do not need to start it when using your existing containers.

For transfers inside Moneta, select your source account and enter the recipient's **10-digit account number**. The backend moves funds between the two accounts atomically.

The interbank form is a simulation until a payment provider is integrated. It records a `PENDING` request with the bank code, recipient name, account number, and amount. It does **not** debit the sender or send money to an external bank.
