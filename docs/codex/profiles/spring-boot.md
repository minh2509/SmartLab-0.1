# Spring Boot Project Profile

## Applies to

Use this profile for Spring Boot REST API, backend, or server-side applications.

## SmartLab notes

For this repository, the backend application lives in `backend/`, uses Java 21 and Spring Boot 4.1, and has its Maven Wrapper inside `backend/`.

The backend package root is `com.smartlab`. The backend runs on port `8080`.

PostgreSQL is the target database, but database integration is currently deferred. Database-independent backend work must use the `nodb` profile.

Required database-independent test command:

```bash
cd backend
SPRING_PROFILES_ACTIVE=nodb ./mvnw clean test
```

Do not add or upgrade backend dependencies without approval.

## Architecture

First inspect the architecture already used by the project.

For a layered web application, use this dependency direction:

```text
Controller
    -> Service
    -> Repository
    -> Entity
```

Do not introduce this architecture into a small project if the assignment or existing code uses a different structure.

SmartLab APIs use request and response DTOs. Do not expose entities directly as API contracts.

## Controller rules

Controllers should:

* Receive and parse requests.
* Validate request structure.
* Call services.
* Return views or API responses.
* Map errors to appropriate responses.

Controllers must not:

* Access repositories directly.
* Contain business workflows.
* Execute database queries.
* Manage transactions.
* Perform all authorization only in the frontend.

## Service rules

Services should:

* Implement business rules.
* Perform business validation.
* Coordinate repositories.
* Define transaction boundaries when needed.
* Enforce resource-level authorization.
* Return clear results or meaningful exceptions.

Services must not:

* Depend directly on HTML templates.
* Duplicate repository query logic.
* Hide unexpected exceptions silently.

## Repository rules

Repositories should:

* Handle persistence operations.
* Define clear query methods.
* Return persisted data.

Repositories must not:

* Implement full business workflows.
* Make presentation decisions.
* Call controllers.
* Send emails or notifications unless the existing architecture explicitly requires it.

## Entity and DTO rules

* Follow the existing entity conventions.
* Use DTOs when the project already separates request, response, form, and persistence models.
* Do not expose password hashes, tokens, audit fields, or unnecessary private data.
* Do not create duplicate DTOs when an appropriate existing DTO already exists.
* Preserve existing entity relationships and database mappings.

## Assignment priority

The assignment requirement has priority over general best practices.

Examples:

* If the assignment requires validation in Service, keep validation in Service.
* If Spring Validation is forbidden, do not introduce it.
* If SQL Server is required, do not migrate to PostgreSQL.
* If `ddl-auto=none` is required, do not change it to `update`.

## Dependencies

Do not add dependencies such as:

* Lombok.
* MapStruct.
* Validation libraries.
* Security libraries.
* Utility libraries.

Unless the task requires them and the user approves them.

## Database safety

Never:

* Drop tables.
* Truncate data.
* Reset the database.
* Replace real credentials.
* Change primary keys.
* Create destructive migrations.

Without explicit approval.

## Verification

Detect whether the project uses Maven or Gradle.

For Maven Wrapper:

```bash
./mvnw test
./mvnw verify
```

On Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```

For Gradle Wrapper:

```bash
./gradlew test
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

Also run:

```bash
git diff --check
```

Do not claim the backend is complete if compilation or required tests fail.
