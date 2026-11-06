# Technische Spezifikation — PIkel

## Architektur

```
Browser (Angular SPA)
  │  /api/v1/**  JSON
  ▼
Quarkus :8080  ── liefert die SPA aus (Quinoa, SPA-Routing)
  ├── adapter/in/rest        JAX-RS-Resources
  ├── application/service    Use Cases (@Transactional)
  ├── domain                 Records, Ports, Regeln
  └── adapter/out/persistence  Panache → PostgreSQL (Schema via Liquibase)
```

- Hexagonal in Backend und Frontend (Details in `CLAUDE.md`)
- Ein deploybares JAR (`target/quarkus-app/quarkus-run.jar`), Container über `src/main/docker/Dockerfile.jvm`
- Konfiguration in `src/main/resources/application.properties`; Produktion über
  `QUARKUS_DATASOURCE_JDBC_URL`, `QUARKUS_DATASOURCE_USERNAME`, `QUARKUS_DATASOURCE_PASSWORD`

## Persistenz

- PostgreSQL; Liquibase (`db/changeLog.xml` → `db/migration/V<n>__<slug>.sql`), `migrate-at-start`
- Hibernate nur `validate`
- Dev und Test: Dev Services (Container `postgres:17`)

## Betrieb

- Health: `/q/health/live`, `/q/health/ready`
- OpenAPI: `/q/openapi`, Swagger UI im Dev-Modus `/q/swagger-ui`

## Offene technische Entscheide

- Auth (OIDC/Keycloak, BFF mit Session-Cookies wie Brainy)
- Echtzeit für das gemeinsame Planungs-Board (SSE oder WebSocket)
- Deployment-Ziel
