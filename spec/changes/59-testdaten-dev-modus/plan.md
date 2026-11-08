# Task — Testdaten im Dev-Modus (Issue #59)

## Übersicht
`./mvnw quarkus:dev` startet mit Beispieldaten: Teams, zwei PIs, Kapazität, Features (Backlog und eingeplant),
Abhängigkeiten und Milestones. Test, Smoke und Produktion bleiben leer.

## Zu erstellende Dateien
- [ ] `src/main/resources/db/dev/dev-data.sql`: Inserts mit festen UUIDs, `ON CONFLICT DO NOTHING`

## Zu ändernde Dateien
- [ ] `src/main/resources/db/changeLog.xml`: am Ende ein changeSet `dev-data` mit `context="dev"` und `runOnChange="true"`
- [ ] `src/main/resources/application.properties`: `quarkus.liquibase.contexts=default` und `%dev.quarkus.liquibase.contexts=default,dev`
- [ ] `docs/arc42.md` Abschnitt 7: Entwicklung startet mit Beispieldaten aus `db/dev/`
- [ ] `.claude/rules/java.md`: Dev-Daten sind keine Migration, sie dürfen geändert werden

Delta: keines — Entwickler-Komfort ohne fachliches Verhalten; keine Story betroffen

## Architektur-Entscheide
- **Liquibase-Kontext statt Java-Loader:** Ein Startup-Bean, das über die Use Cases einfügt, müsste in einem
  eigenen Adapter-Paket liegen. Das verbietet `driving_ports_are_implemented_only_by_application`
  (nur REST darf Use Cases aufrufen). SQL umgeht zwar die Domain-Validierung, die DB-Constraints greifen aber.
  Die Daten wählen wir gültig nach den Domain-Regeln: keine Features in der IP-Iteration, Rang ≥ 1.
- **Kontext immer gesetzt:** Ohne gesetzte Kontexte führt Liquibase *alle* changeSets aus, auch die mit `dev`.
  Erst `contexts=default` in jedem Profil schliesst sie aus.
- **Eigener Ordner `db/dev/`, `runOnChange`, idempotent:** Es ist keine Schema-Migration (Regel 3 gilt nicht).
  Die Daten sollen mit dem Schema mitwachsen: Eine Änderung wird beim nächsten Start neu angewendet, ohne Checksum-Fehler und
  ohne Duplikate. Das changeSet steht am Ende; neue Migrationen kommen davor.
- Keine ADR nötig, kein neues Paket, kein Java-Code.

## Aufgaben
- [x] 1. Kontext-Konfiguration in `application.properties` — erledigt, wenn `scripts/gate.sh --fast` grün ist
- [x] 2. `dev-data.sql` und das changeSet (Teams Adler, Falke, Luchs; PI 27.1 mit Kapazität, 8 Features, davon 4 eingeplant,
  2 Abhängigkeiten und 2 Milestones; PI 27.2 mit 3 Features im Backlog) — erledigt, wenn `quarkus:dev` startet und
  `GET /api/v1/program-increments` sowie das Board von PI 27.1 die Daten zeigen
- [x] 3. Zweiter Start (Live-Reload und Neustart) ohne Fehler und ohne Duplikate — erledigt bei gleicher Anzahl Zeilen
- [x] 4. arc42 und `java.md` nachführen
- [x] 5. `scripts/gate.sh` grün: `CucumberIT` und `SmokeIT` sehen keine Dev-Daten. «eine leere Teamliste» würde
  sonst am FK `feature.team_id` scheitern

## Test-Strategie
- [ ] Integration: Die bestehenden `CucumberIT`-Szenarien (Teamliste leeren, PI-Liste) und `SmokeIT` bleiben grün.
  Damit ist belegt, dass Test- und Prod-Profil den Kontext `dev` nicht laden
- [ ] Von Hand: `quarkus:dev` starten, PI-Liste, Teams, Board von PI 27.1 mit Linien und Milestones, Kapazität
- [ ] Kein neuer automatischer Test (siehe offene Frage)

## Offene Fragen
- Ein automatischer Test, der die Dev-Daten gegen das Schema lädt (`@QuarkusTest` mit eigenem Profil und eigener
  Dev-Services-DB)? **Empfehlung: nein.** Bricht eine Migration die Daten, merkt es der nächste `quarkus:dev`-Start
  sofort. Ein zusätzlicher Quarkus-Neustart verlängert dagegen jeden Integrationslauf
