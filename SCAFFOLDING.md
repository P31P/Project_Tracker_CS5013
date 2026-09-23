# Scaffolding: creating the project skeleton

This covers handoffs **H1** (Maven skeleton + CI) and **H2** (`LlmClient`) from [plans/SUJAL.md](plans/SUJAL.md). It builds only the empty project that both of you work on. The schema (H4), security (H6) and all features come later.

It includes only the dependencies the skeleton needs. Every later task adds its own libraries to `pom.xml` and `requirements.txt` in the same PR.

When you've finished, `./mvnw verify` passes locally and on GitHub, and the app starts on a local Postgres.

| Piece | Version |
|---|---|
| Java | 21 (LTS) |
| Spring Boot | 4.1.1 |
| Zonky `embedded-postgres` | 2.2.2 (Postgres binaries 18.6.0) |

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
git checkout -b d/scaffold

BASE=src/main/java/in/ac/iitm/ramanlab/tracker
TEST=src/test/java/in/ac/iitm/ramanlab/tracker

mkdir -p $BASE/{config,common,auth,data,projects,llm,autofill,slack,papers,search}
mkdir -p src/main/resources/{db/migration,static/{css,js}}
mkdir -p $TEST/{support,llm}
mkdir -p .github/workflows
touch src/main/resources/db/migration/.gitkeep
```

Sub-packages (`data/entity`, `papers/resolver`, …) and folders like `deploy/` and `fixtures/` are created by the task that first needs them.

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

    <groupId>in.ac.iitm.ramanlab</groupId>
    <artifactId>tracker</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <name>Raman Lab Tracker</name>

    <properties>
        <java.version>21</java.version>
        <embedded-postgres.version>2.2.2</embedded-postgres.version>
        <embedded-postgres-binaries.version>18.6.0</embedded-postgres-binaries.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- keep the major version equal to the lab server's Postgres -->
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

Then generate the Maven wrapper. After that, nobody else needs Maven installed:

```bash
mvn -N wrapper:wrapper
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

llm:
  provider: ${LLM_PROVIDER:disabled}
```

The `.env` import lets `./mvnw spring-boot:run` pick up local settings. Later tasks add their own sections (`slack:`, Google OAuth, and so on).

**`src/main/resources/application-local.yml`**

```yaml
logging:
  level:
    in.ac.iitm.ramanlab.tracker: DEBUG
```

**`src/test/resources/application-test.yml`**

```yaml
llm:
  provider: disabled
```

**`.env.example`**: commit this one. Copy it to `.env`, which git ignores.

```bash
DB_URL=jdbc:postgresql://localhost:5432/tracker
DB_USER=tracker
DB_PASSWORD=tracker
LLM_PROVIDER=disabled
```

---

## Step 4: Application class

**`$BASE/TrackerApplication.java`**

```java
package in.ac.iitm.ramanlab.tracker;

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

## Step 5: Package owners (`package-info.java`)

Each module package gets one of these. It keeps the empty folder in git and records the owner.

```java
/** Module A: Slack ingestion. Owner: Priyal. */
package in.ac.iitm.ramanlab.tracker.slack;
```

| Package | Module | Owner |
|---|---|---|
| `config`, `common`, `auth`, `data` | D | Sujal |
| `projects` | E | Sujal |
| `llm`, `autofill` | F | Sujal |
| `slack` | A | Priyal |
| `papers` | B | Priyal |
| `search` | C | Priyal |

---

## Step 6: `LlmClient` (H2)

**`$BASE/llm/LlmClient.java`**

```java
package in.ac.iitm.ramanlab.tracker.llm;

/** Returns the model's raw reply; callers parse and validate it. */
public interface LlmClient {

    String complete(String prompt);
}
```

**`$BASE/llm/LlmUnavailableException.java`**

```java
package in.ac.iitm.ramanlab.tracker.llm;

public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message) {
        super(message);
    }

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**`$BASE/llm/DisabledLlmClient.java`**

```java
package in.ac.iitm.ramanlab.tracker.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "disabled", matchIfMissing = true)
public class DisabledLlmClient implements LlmClient {

    @Override
    public String complete(String prompt) {
        throw new LlmUnavailableException("LLM is disabled (llm.provider=disabled)");
    }
}
```

`HostedLlmClient` (H8) and `LocalLlmClient` come later, using `havingValue = "hosted"` and `"local"`.

---

## Step 7: Tests

**`$TEST/support/PostgresTestBase.java`**: database tests extend this class. It starts one real Postgres for the whole test run.

```java
package in.ac.iitm.ramanlab.tracker.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
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
package in.ac.iitm.ramanlab.tracker;

import in.ac.iitm.ramanlab.tracker.support.PostgresTestBase;
import org.junit.jupiter.api.Test;

class TrackerApplicationTests extends PostgresTestBase {

    @Test
    void contextLoads() {
    }
}
```

**`$TEST/llm/DisabledLlmClientTest.java`**

```java
package in.ac.iitm.ramanlab.tracker.llm;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DisabledLlmClientTest {

    @Test
    void refusesEveryPrompt() {
        assertThatThrownBy(() -> new DisabledLlmClient().complete("anything"))
                .isInstanceOf(LlmUnavailableException.class);
    }
}
```

Embedded Postgres won't run as root, so don't run the tests with `sudo`.

---

## Step 8: Dashboard placeholder

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

## Step 9: CI

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
      - run: ./mvnw -B verify
```

After the first merge, turn on branch protection for `main`: require the `build` check and one review.

---

## Step 10: Check it works

```bash
cp .env.example .env
./mvnw verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

`http://localhost:8080` should show "Raman Lab Tracker is running."

Leave the changes for review and open a PR from `d/scaffold`. Once it's merged, H1 and H2 are done.

---

## Added later, by the task that needs it

| Library | Task | Owner |
|---|---|---|
| security, OAuth2 client, security-test | Phase 0, H6 sign-in | Sujal |
| validation, webmvc-test | Phase 1 project API | Sujal |
| Bolt Jakarta Socket Mode, Tyrus | Phase 1 `SlackAppConfig` | Priyal |
| WireMock | Phase 1 resolvers | Priyal |
| Jsoup | Phase 2 meta-tag rung | Priyal |
| PDFBox | Phase 2 PDF rung | Priyal |
| actuator | Phase 3 health check | Sujal |
