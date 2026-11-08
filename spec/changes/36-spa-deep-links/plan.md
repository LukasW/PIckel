# Task — Direktaufruf und Reload von SPA-Unterseiten liefern 404 (Issue #36)

## Übersicht

`/teams`, `/program-increments/<id>` und `/program-increments/<id>/board` liefern bei einem Direktaufruf oder Reload
404 mit leerem Body, im Dev-Modus **und im gepackten JAR** (geprüft am 2026-11-08 auf `85a7132` mit eigener
PostgreSQL). Die Navigation innerhalb der App ist nicht betroffen.

**Ursache (belegt):** `UnreadableJsonExceptionMapper` (seit #2) ist ein `ExceptionMapper<WebApplicationException>`.
Findet Quarkus REST zu einem Pfad keine Resource, prüft `ClassRoutingHandler`, ob es einen Mapper für
`NotFoundException` gibt. Gibt es einen, auch über einen Obertyp, beantwortet REST die Anfrage selbst mit 404.
Ohne Mapper reicht REST die Anfrage weiter, und der SPA-Routing-Handler von Quinoa liefert `index.html` aus. Weil
REST unter `/` hängt, landen alle Pfade zuerst dort.

Spike in einem Worktree (verworfen, nicht committet):

| Variante | `/teams` | kaputtes JSON an `POST /api/v1/teams` |
|---|---|---|
| Stand `main` | 404 | 400 mit `{"message":"Die Anfrage ist kein gültiges JSON."}` |
| Mapper entfernt | 200 | 400 **ohne** Fehlerkörper (bricht ADR 0003) |
| `quarkus.rest.path=/api`, Mapper bleibt | 200 | 400 mit Fehlerkörper; `/q/openapi` zeigt weiter `/api/v1/**` |

**Lösung:** REST nur noch unter `/api` hängen (`quarkus.rest.path=/api`), die `@Path` der Resources um `/api`
kürzen. Pfade ausserhalb von `/api` erreichen REST dann gar nicht mehr, der Mapper bleibt unverändert. Die
öffentlichen URLs ändern sich nicht.

## Zu erstellende Dateien

keine

## Zu ändernde Dateien

- [x] `src/main/resources/application.properties` — `quarkus.rest.path=/api` mit Kommentar, warum (SPA-Routing)
- [x] `src/main/java/ch/grudligstrasse/pickel/adapter/in/rest/*Resource.java` (7 Dateien) — `@Path("/api/v1/…")` →
  `@Path("/v1/…")`
- [x] `src/test/java/ch/grudligstrasse/pickel/SmokeIT.java` — Direktaufruf einer Unterseite liefert die SPA aus

## Delta

Delta: keines — Bugfix ohne fachliche Änderung; keine Story beschreibt Direktaufrufe, die REST-URLs bleiben gleich

## Architektur-Entscheide

- Nur Adapter (`adapter/in/rest`) und Konfiguration; Domain und Application unberührt, `ArchitectureTest` bleibt grün
- **Verworfen:** den Mapper umbauen. Quarkus wirft beim JSON-Parse-Fehler eine nackte `WebApplicationException`;
  jeder Mapper, der sie fängt, ist auch ein Mapper für `NotFoundException`. Ein eigener `MessageBodyReader` wäre
  mehr Code für dasselbe Ergebnis
- **Verworfen:** Quinoa-Handler vor REST einhängen; dafür gibt es keine Konfiguration
- Keine ADR nötig: Die API-Pfade (`/api/v1/**`, arc42 Kapitel 3) bleiben gleich. Der Kommentar in
  `application.properties` hält den Grund fest, damit niemand `quarkus.rest.path` wieder entfernt

## Aufgaben

- [x] 1. `SmokeIT` um einen Test erweitern: `GET /teams` und `GET /program-increments/<beliebige-uuid>/board` mit
  `Accept: text/html` liefern 200 und `text/html` — erledigt, wenn der Test auf `main` rot ist (404)
- [x] 2. `quarkus.rest.path=/api` setzen und die sieben `@Path` kürzen — erledigt, wenn `SmokeIT` grün ist und alle
  `CucumberIT`-Szenarien unverändert grün sind (sie rufen weiter `/api/v1/**` auf)
- [x] 3. Im Dev-Modus `/teams` direkt aufrufen — erledigt, wenn 200 statt 404

## Test-Strategie

- [x] Smoke: Direktaufruf von `/teams` und `/program-increments/<uuid>/board` liefert im gepackten JAR `index.html`
  (200, `text/html`)
- [x] Smoke: `GET /` bleibt 200 (bestehender Test)
- [x] Integration: Alle bestehenden `CucumberIT`-Szenarien bleiben unverändert grün; sie belegen, dass die
  REST-URLs gleich bleiben, auch die 400 mit Fehlerkörper bei kaputtem JSON (Szenario in
  `features/program-increments/1-pi-anlegen.feature`)
- [x] Unit: keine neuen; die Mapper-Logik ändert sich nicht
- [x] Von Hand: Reload auf dem Board im Dev-Modus (Quinoa-Proxy ist ein anderer Weg als das JAR)

## Offene Fragen

keine
