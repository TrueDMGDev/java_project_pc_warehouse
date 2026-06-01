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

Create a database user that can create the application schema:

```sql
CREATE USER app WITH PASSWORD 'a';
CREATE DATABASE pc_warehouse OWNER app;
```

If the database already exists under another owner, grant the app user schema-create permission:

```sql
GRANT CREATE ON DATABASE pc_warehouse TO app;
```

Update `src/main/resources/application.properties` with your PostgreSQL database settings:

```properties
db.url=jdbc:postgresql://localhost:5432/pc_warehouse
db.username=app
db.password=a
db.schema=app
```
