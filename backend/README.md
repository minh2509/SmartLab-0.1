# SmartLab Backend

Backend API for the SmartLab Management System.

## Technology

- Java 21
- Spring Boot 4.1
- Maven Wrapper
- Spring Web MVC
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Spring Boot Actuator

## Root package

```text
com.smartlab
```

## Compile

From the `backend` directory:

```bash
./mvnw clean compile
```

## Run without database

```bash
SPRING_PROFILES_ACTIVE=nodb ./mvnw spring-boot:run
```

## Run locally with PostgreSQL

Create a local `.env` file from `.env.example` and set your real local database password.

`.env` is ignored by Git. Spring Boot does not automatically read `.env`; export it in the shell before starting the backend:

```bash
cd backend
set -a
source .env
set +a
./mvnw spring-boot:run
```

To run the full monorepo with the local backend profile from the repository root:

```bash
set -a
source backend/.env
set +a
npm run dev
```

## Health check

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{
  "status": "UP"
}
```

## Environment variables

```text
SERVER_PORT
DB_URL
DB_USERNAME
DB_PASSWORD
SMARTLAB_JWT_SECRET_BASE64
SMARTLAB_JWT_ACCESS_TTL_SECONDS
SMARTLAB_ADMIN_JOIN_REQUEST_OVERRIDE_ENABLED
```

`SMARTLAB_ADMIN_JOIN_REQUEST_OVERRIDE_ENABLED` defaults to `false`. Set it to `true` only when Admins are allowed to approve or reject project join requests as an override.

## Admin API smoke collection

Import `postman/SmartLab-Admin-API.postman_collection.json` into Postman, set the Admin login variables, and run the folders in order against the local PostgreSQL profile. The login request stores the Bearer token and lab/user identifiers for the remaining Admin requests.

Do not commit real database credentials.
