# US-8: Business-Kontext und ART-Ziele am PI

**ID:** US-8
**Issue:** [#8](https://github.com/LukasW/PIkel/issues/8)
**Status:** Umgesetzt
**Priorität:** Tief
**Schätzung:** 3

## Beschreibung

Als RTE möchte ich den Business-Kontext und die PI Objectives des ART als Text und Links am PI hinterlegen, damit
alle Teams im Planning dieselbe Ausgangslage sehen.

## Akzeptanzkriterien

1. Am PI lassen sich ein Business-Kontext und die PI Objectives des ART als Text erfassen und ändern, je höchstens
   10 000 Zeichen
2. Am PI lassen sich Links mit Titel (Pflicht, höchstens 100 Zeichen) und URL (Pflicht, `http` oder `https`)
   hinzufügen und entfernen
3. Die Detailseite des PI zeigt Text mit Zeilenumbrüchen und die Links, die sich in einem neuen Tab öffnen
4. Ohne Inhalt zeigt der Bereich einen Hinweis, dass noch kein Business-Kontext erfasst ist

## BDD-Szenarien

Siehe [`src/test/resources/features/program-increments/8-business-kontext.feature`](../../src/test/resources/features/program-increments/8-business-kontext.feature).

## Tests

- AK 1 (Text erfassen, höchstens 10 000 Zeichen): JUnit `domain/model/ProgramIncrementTest`,
  `application/service/ProgramIncrementServiceTest`; CucumberIT «Business-Kontext wird erfasst», «Business-Kontext
  wird geändert»; Vitest
  `program-increment-detail.component.spec.ts`, `program-increments.service.spec.ts`
- AK 2 (Links hinzufügen und entfernen, Titel höchstens 100 Zeichen, nur `http`/`https`): JUnit
  `domain/model/PiLinkTest`, `ProgramIncrementTest`, `ProgramIncrementServiceTest`; CucumberIT «Link wird
  hinzugefügt», «Link wird entfernt», «Link ohne http oder https wird abgelehnt»; Vitest Detailseite und Service
- AK 3 und 4 (Detailseite): Vitest `program-increment-detail.component.spec.ts` (Testnamen mit Suffix `(US-8)`)
- Szenario «Zu langer Business-Kontext wird abgelehnt» (`@Unit`): JUnit `ProgramIncrementTest →
  rejectsBusinessContextLongerThan10000Characters`

## Änderungen

- #8: Business-Kontext und ART-Ziele am PI — `spec/changes/8-business-kontext/delta.md`
