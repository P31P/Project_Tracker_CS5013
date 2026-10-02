# Scaffolding: creating the project skeleton

This covers handoff **H1** (Maven skeleton + CI) from [plans/SUJAL.md](plans/SUJAL.md). It builds only the empty project that both of you work on. Everything else comes later, one task at a time.

It includes only the dependencies the skeleton needs. Every later task adds its own libraries to `pom.xml` and `requirements.txt` in the same PR.

When you've finished, `mvn verify` passes locally and on GitHub, and the app starts on a local Postgres.

| Piece | Version |
|---|---|
| Java | 21 (LTS) |
| Spring Boot | 4.1.1 |
| Zonky `embedded-postgres` | 2.2.2 (Postgres binaries 16.15.0) |

---

## What the scaffold creates

```
Project_Tracker_CS5013/
├── pom.xml
├── .github/workflows/ci.yml
└── src/
    ├── main/
    │   ├── java/tracker/
    │   │   └── TrackerApplication.java
    │   └── resources/
    │       ├── application.yml
    │       ├── application-local.yml
    │       ├── db/migration/.gitkeep
    │       └── static/
    │           ├── index.html
    │           ├── css/app.css
    │           └── js/app.js
    └── test/java/tracker/
        ├── TrackerApplicationTests.java
        └── support/PostgresTestBase.java
```

| File | Step | Purpose |
|---|---|---|
| `pom.xml` | 2 | Build file with scaffold dependencies only |
| `application.yml` | 3 | Shared config; secrets come from env / `.env` |
| `application-local.yml` | 3 | Debug logging for local runs |
| `TrackerApplication.java` | 4 | Spring Boot entry point |
| `support/PostgresTestBase.java` | 5 | Base class that starts embedded Postgres for tests |
| `TrackerApplicationTests.java` | 5 | Checks the app starts |
| `db/migration/.gitkeep` | 1 | Placeholder until `V1__init.sql` (H4) |
| `static/index.html`, `app.css`, `app.js` | 6 | Placeholder page, replaced in H7 |
| `.github/workflows/ci.yml` | 7 | Runs `mvn verify` on pushes and PRs |

---

## Step 0: Prerequisites

See [requirements.txt](requirements.txt).

```bash
java -version
mvn -version
psql --version
```

Local database, once per laptop:

```bash
sudo -u postgres psql -c "CREATE USER tracker WITH PASSWORD 'tracker';"
sudo -u postgres psql -c "CREATE DATABASE tracker OWNER tracker;"
```

---

## Step 1: Branch and folders

```bash
git checkout -b scaffold

BASE=src/main/java/tracker
TEST=src/test/java/tracker

mkdir -p $BASE
mkdir -p src/main/resources/{db/migration,static/{css,js}}
mkdir -p $TEST/support
mkdir -p .github/workflows
touch src/main/resources/db/migration/.gitkeep
```

Module packages (`auth`, `data`, `slack`, …) and folders like `deploy/` are created by the task that first needs them.

---

## Step 2: `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.1</version>
        <relativePath/>
    </parent>

    <groupId>ramanlab</groupId>
    <artifactId>tracker</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <name>Raman Lab Tracker</name>

    <properties>
        <java.version>21</java.version>
        <embedded-postgres.version>2.2.2</embedded-postgres.version>
        <embedded-postgres-binaries.version>16.15.0</embedded-postgres-binaries.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- keep the major version equal to the Postgres we run against -->
            <dependency>
                <groupId>io.zonky.test.postgres</groupId>
                <artifactId>embedded-postgres-binaries-bom</artifactId>
                <version>${embedded-postgres-binaries.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-flyway</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.zonky.test</groupId>
            <artifactId>embedded-postgres</artifactId>
            <version>${embedded-postgres.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Step 3: Configuration

**`src/main/resources/application.yml`**

```yaml
spring:
  application:
    name: tracker
  config:
    import: optional:file:.env[.properties]
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/tracker}
    username: ${DB_USER:tracker}
    password: ${DB_PASSWORD:tracker}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate  # Flyway owns the schema

server:
  port: ${PORT:8080}
```

The `.env` import lets `mvn spring-boot:run` pick up local settings. Later tasks add their own sections.

**`src/main/resources/application-local.yml`**

```yaml
logging:
  level:
    tracker: DEBUG
```

---

## Step 4: Application class

**`$BASE/TrackerApplication.java`**

```java
package tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrackerApplication.class, args);
    }
}
```

---

## Step 5: Tests

**`$TEST/support/PostgresTestBase.java`**: database tests extend this class. It starts one real Postgres for the whole test run.

```java
package tracker.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
public abstract class PostgresTestBase {

    private static final EmbeddedPostgres POSTGRES = start();

    private static EmbeddedPostgres start() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not start embedded Postgres", e);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }
}
```

**`$TEST/TrackerApplicationTests.java`**

```java
package tracker;

import tracker.support.PostgresTestBase;
import org.junit.jupiter.api.Test;

class TrackerApplicationTests extends PostgresTestBase {

    @Test
    void contextLoads() {
    }
}
```

Embedded Postgres won't run as root, so don't run the tests with `sudo`.

---

## Step 6: Dashboard placeholder

**`src/main/resources/static/index.html`**: the real shell replaces it in H7.

```html
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Raman Lab Tracker</title>
    <link rel="stylesheet" href="css/app.css">
</head>
<body>
    <main id="app">Raman Lab Tracker is running.</main>
    <script type="module" src="js/app.js"></script>
</body>
</html>
```

Create empty `static/css/app.css` and `static/js/app.js` alongside it.

---

## Step 7: CI

**`.github/workflows/ci.yml`**

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - run: mvn -B verify
```

After the first merge, turn on branch protection for `main`: require the `build` check and one review.

---

## Step 8: Check it works

```bash
mvn verify
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

`http://localhost:8080` should show "Raman Lab Tracker is running."

Leave the changes for review and open a PR from `scaffold`. Once it's merged, H1 is done.
