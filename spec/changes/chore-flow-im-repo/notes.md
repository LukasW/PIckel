# Notizen — chore/flow-im-repo

## Überraschungen

- PIT fand beim ersten Lauf 67 %: Grenze von genau 100 Zeichen, `listAll` und die Daten nach `plan` waren
  ungetestet. Der test-reviewer fand danach drei weitere Lücken, die PIT mit den Standard-Mutatoren nicht sieht:
  Name `null`, Enddatum `null`, Enddatum vor Startdatum. Alle ergänzt
- `check-story-drift.sh`: `«$title»` liest Bash als Variable `title»` (UTF-8-Byte im Namen); `${title}` nötig
- Der global installierte Plugin-Hook `guard-env.sh` lief in derselben Session noch mit und blockierte Befehle,
  die `.env` nur im Text nennen

## Reviewer-Befunde

- hexagonal-reviewer: ArchitectureTest hatte Lücken (Denylist statt Allowlist für die Domain, `@Transactional`,
  Adapter untereinander, Panache/JPA ausserhalb der Persistenz, ExceptionMapper, Ports als Interfaces, Namen,
  neue Pakete). Alle übernommen, 14 Regeln
- test-reviewer: «`strip()` ungetestet» verworfen — `planTrimsName` prüft `"  PI 27.1 "`
- test-reviewer: AK 2 von US-1 (Sortierung nach Startdatum) belegt kein Test mit unsortierten Eingaben. Bestehende
  Lücke von US-1, nicht Teil dieses Chores → eigenes Issue

## Offen

- Szenario für AK 2 (Sortierung) in `1-pi-anlegen.feature` als eigenes Issue
