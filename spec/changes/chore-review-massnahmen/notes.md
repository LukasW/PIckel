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

## Ersatz für Branch Protection

GitHub bietet Branch Protection und Rulesets für dieses private Repository im aktuellen Plan nicht an (HTTP 403,
«Upgrade to GitHub Pro or make this repository public»). Ersatz im Repository: `guard-push.sh` (PreToolUse) lehnt
Push auf `main` (auch `HEAD:refs/heads/main`, `HEAD` auf `main`, Push ohne Refspec auf `main`), `+refspec`, Force-
und Delete-Optionen ab; `test-guard-push.sh` in Gate und CI. Das schützt nur vor dem Agenten, nicht vor Menschen
oder anderen Werkzeugen.

## Bewusst nicht umgesetzt

- **PIT auf den Persistenz-Adapter:** Panache braucht die Bytecode-Anreicherung von Quarkus; abgesichert über das
  Sortier-Szenario in `CucumberIT` statt über Mutation
- **Abhängigkeits-Scan per `dependency-review-action`:** Das Repository ist privat und hat kein GitHub Advanced
  Security; stattdessen `npm audit` und Renovate mit OSV
- **`cp`/`mv` im Nachtlauf sperren:** Präfix-Regeln können Zielpfade nicht einschränken; die Sperre gegen Edits an
  `.claude/` usw. bleibt damit umgehbar. Grenze bleiben `guard-push.sh`, das Label
  `needs-human-review` aus CI und das Review am Morgen

## Offen

- Echte Branch Protection braucht GitHub Pro oder ein öffentliches Repository: Entscheid des Nutzers
- Regelwerk nach der zweiten Story ausdünnen
