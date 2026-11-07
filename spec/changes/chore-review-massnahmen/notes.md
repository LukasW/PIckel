# Notizen — chore/review-massnahmen

## Überraschungen

- Quarkus REST verpackt einen JSON-Lesefehler (`StreamReadException`) in eine `WebApplicationException(400)`; ein
  Mapper für `JsonParseException` greift deshalb nie. Gelöst mit einem Mapper für `WebApplicationException`, der nur
  diesen Fall umschreibt
- Eine Anfrage ohne Inhalt kommt als `null` an und löst `@NotNull` aus; deshalb bleibt diese eine Annotation und
  bekommt einen eigenen Mapper
- Die Sortierung nach Startdatum war schon korrekt; das neue Szenario ist eine Absicherung, kein Fehlerfund
- `guard-labels.sh` blockierte beim Arbeiten Bash-Befehle, die das Label zum Übersteuern des Spec-Drift-Gates nur im
  Text nannten (wie vorgesehen); Testfälle und Notizen deshalb als Datei
- `--disallowedTools` ist variadisch, und eine Regel mit Leerzeichen zerfällt beim Aufsplitten; der Nachtlauf erzeugt
  deshalb je Phase eine Settings-Datei mit `jq`
- macOS liefert `jq` unter `/usr/bin` mit; der Test «ohne jq» baut sich einen eigenen PATH

## Reviewer-Befunde

- hexagonal-reviewer, angular-signals-reviewer: keine Befunde
- test-reviewer: Formular-Erfolgsweg, Grenze 100 Zeichen im Formular, Fehlerpfad von `plan` ohne Neuladen, Meldung
  bei Enddatum gleich Startdatum, GraphQL-`removeLabelsFromLabelable` und `gh label delete` im Guard-Test — alle
  übernommen
- spec-reviewer: Formular mass die Länge vor dem Trimmen (Server danach) → `maxTrimmedLength`, zuerst rot gesehen;
  «am Feld» war nur über den ganzen Seitentext belegt → Test sucht im `label` des Felds und prüft `aria-invalid`.
  Beide übernommen

## Branch Protection

Für das private Repository bot GitHub im aktuellen Plan weder Branch Protection noch Rulesets an (HTTP 403). Der
Nutzer hat das Repository deshalb am 7. November 2026 öffentlich gemacht; vorher fand Gitleaks in allen 8 Commits
nichts, eine `.env` war nie versioniert. Seither gilt auf `main`:

- Pflicht-Checks `Unit Tests`, `Integration + Smoke`, `Spec-Drift`, `Secret-Scan (Gitleaks)`; Branch muss nicht
  aktuell sein, sonst baut der Nachtlauf mit `merge: ja` vor jedem Merge neu
- Nur per PR, 0 Pflicht-Freigaben: Bei einer Person kann niemand den eigenen PR freigeben
- Gilt auch für Admins; lineare Historie; kein Force-Push, kein Löschen; offene Kommentare müssen aufgelöst sein

`guard-push.sh` bleibt: Er hält den Agenten schon lokal an, bevor GitHub ablehnt. Das Label `needs-human-review`
erzwingt GitHub nicht, daran hält sich nur `/flow-ship`.

## Bewusst nicht umgesetzt

- **PIT auf den Persistenz-Adapter:** Panache braucht die Bytecode-Anreicherung von Quarkus; abgesichert über das
  Sortier-Szenario in `CucumberIT` statt über Mutation
- **Abhängigkeits-Scan per `dependency-review-action`:** Beim Umsetzen war das Repository privat, ohne GitHub
  Advanced Security; stattdessen `npm audit` und Renovate mit OSV. Seit es öffentlich ist, wäre die Action möglich
- **`cp`/`mv` im Nachtlauf sperren:** Präfix-Regeln können Zielpfade nicht einschränken; die Sperre gegen Edits an
  `.claude/` usw. bleibt damit umgehbar. Grenze bleiben Branch Protection, `guard-push.sh`, das Label
  `needs-human-review` aus CI und das Review am Morgen

## Offen

- `dependency-review-action` in `ci-pr.yml`, seit das Repository öffentlich ist
- Regelwerk nach der zweiten Story ausdünnen
