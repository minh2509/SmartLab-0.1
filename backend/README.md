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
```

Do not commit real database credentials.
