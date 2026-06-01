# PC Warehouse

Starter desktop application scaffold using JavaFX, Maven, JDBC, and PostgreSQL.

## Stack

- Java 21
- Maven
- JavaFX 21
- PostgreSQL JDBC driver

## Project Layout

- `src/main/java` for application code
- `src/main/resources` for FXML, CSS, and configuration
- `src/test/java` ready for tests when you add them

## Run

```bash
mvn clean javafx:run
```

## Database Setup

Update `src/main/resources/application.properties` with your PostgreSQL database settings:

```properties
db.url=jdbc:postgresql://localhost:5432/pc_warehouse
db.username=postgres
db.password=postgres
```
