# PIckel

Werkzeug für das **PI Planning** (SAFe) — aktuell ein Walking Skeleton: Program Increments anlegen und auflisten,
durch alle Schichten von Angular bis PostgreSQL.

Quarkus 3.40 + Angular 22 in einem deploybaren JAR (Quinoa). Die SPA liegt unter `src/main/webapp`.

## Voraussetzungen

- Java 25, Node aus `.nvmrc`, npm 11
- Podman oder Docker (Dev Services starten PostgreSQL)

## Entwicklung

```bash
cd src/main/webapp && npm ci && cd -   # einmal nach dem Klonen
./mvnw quarkus:dev                     # http://localhost:8080 (Hot-Reload für Java und Angular)
```

Swagger UI: http://localhost:8080/q/swagger-ui

## Tests

```bash
./mvnw test -B      # JUnit + Vitest
./mvnw verify -B    # + CucumberIT und SmokeIT gegen Dev-Services-PostgreSQL
```

## Produktion

```bash
./mvnw package
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://host:5432/pickel \
QUARKUS_DATASOURCE_USERNAME=pickel QUARKUS_DATASOURCE_PASSWORD=… \
java -jar target/quarkus-app/quarkus-run.jar
```

## Mit Claude Code arbeiten

Siehe [`CLAUDE.md`](CLAUDE.md) (Architektur, Konventionen) und [`claude-guide.md`](claude-guide.md) (Ablauf mit
den Skills `/flow-*`, Reviewer, Hooks).
