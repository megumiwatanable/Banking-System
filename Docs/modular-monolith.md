# Modular monolith structure

The backend remains one Spring Boot application, one Maven artifact, and one database. Code is grouped by business capability so each module owns its API, application logic, domain model, and persistence adapter.

```text
com.banking
├── account
│   ├── api
│   │   └── dto
│   ├── application
│   ├── domain
│   └── infrastructure
├── transaction
│   ├── api
│   │   └── dto
│   ├── application
│   ├── domain
│   └── infrastructure
├── user
│   ├── api
│   │   └── dto
│   ├── application
│   ├── domain
│   └── infrastructure
├── customer       # CIF/KYC customer profile
├── casa           # CASA package enrollment
├── asset          # self-declared customer asset portfolio
├── credit         # internal indicative credit rating
├── citad          # simulated interbank inquiry workflow
└── shared
    ├── config
    ├── error
    ├── ratelimit
    └── security
```

## Dependency direction

- API controllers call their module's application service.
- Application services coordinate domain entities and repositories.
- Repositories are persistence adapters under `infrastructure`.
- Modules may reference another module's domain type when the current relational model requires it. Account references User; Transaction references Account.
- Cross-cutting HTTP errors, security, configuration, and rate limiting live under `shared`.
- Architecture tests prevent API packages from importing infrastructure, keep domain packages free of application and adapter dependencies, and prevent `shared` from depending on a business module.
- No module is deployed separately. Extracting a module into a service later requires replacing its direct entity/repository dependency with an explicit API or event contract.

This is a pragmatic modular monolith rather than a set of fully isolated modules. Some application services query repositories owned by another module, and several JPA entities have cross-module relationships. Those dependencies keep the current relational workflows simple, but they should be replaced with module-facing application interfaces or events before a module is extracted into a separate service.

Database migrations stay under `src/main/resources/db/migration` and continue to run once for the application.

## Financial service MVPs

- **CASA:** enrolls an authenticated customer using an owned `CURRENT` settlement account.
- **Transfers:** internal transfers settle atomically; interbank transfers remain simulated and `PENDING` until a real payment rail is integrated.
- **Customer profile:** assigns a CIF-style customer number and stores contact/KYC profile data.
- **Credit rating:** calculates an internal indicative score from application balances, self-declared assets, and successful transactions. It is not a CIC score or lending decision.
- **Asset management:** CRUD portfolio for customer-declared assets and estimated values.
- **CITAD inquiry:** opens and tracks an inquiry against an owned interbank transaction. The workflow is simulated and does not call the external CITAD network.
