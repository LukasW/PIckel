# Notizen #16

- Nachtlauf ohne `plan.md`/`entscheide.md` im Change-Ordner: Plan in Schritt 4 selbst geschrieben
- Annahme: Prüfreihenfolge Selbstbezug → anderes PI → doppelt → umgekehrt. Grund: Die Meldung soll den
  grundlegendsten Fehler nennen; belegt in `FeatureDependencyTest` (`…IsReportedBefore…`)
- Annahme: Brauchendes Feature aus einem anderen PI als dem der Adresse → 404 (wie bei Features, US-10); das
  gebrauchte Feature aus einem anderen PI → 400 mit Meldung aus dem Delta. Belegt in `FeatureDependencyServiceTest`
- Annahme: Entfernen einer Abhängigkeit, die es nicht gibt, ist idempotent (204). Grund: Das Ziel «keine
  Abhängigkeit» ist erreicht; kein eigener NotFound-Typ nötig. Belegt in `removeOfUnknownDependencyIsNoError`
- Annahme: «Detailseite» im Delta = Backlog auf der Detailseite des PI (eine Detailseite pro Feature gibt es nicht)
- Annahme: Titel in «Braucht»/«Wird gebraucht von» und die Auswahl in Backlog-Reihenfolge; die Auswahl bietet alle
  anderen Features an, die Regeln prüft der Server; nach Erfolg springt die Auswahl auf leer
- Kaskade beim Löschen über `ON DELETE CASCADE` in der Datenbank statt im Service: eine Regel an einem Ort, wirkt
  auch für künftige Löschwege. Darum belegt nur `CucumberIT` AK 5 im Backend
- Review: Entfernen über die Adresse eines anderen PI war möglich; jetzt 404, wenn das brauchende Feature zu einem
  anderen PI gehört (`removeRejectsNeedingFeatureOfOtherProgramIncrementAsNotFound`)
- Review: Kaskade beim Löschen des brauchenden Features war ungetestet; Szenario «Gelöschtes brauchendes Feature
  nimmt seine Abhängigkeiten mit» unter «Neu» im Delta ergänzt
- Persona neutral («Teilnehmerin oder Teilnehmer des PI Planning» wie US-12); das Delta nennt keine Rolle
- Fehlende Feature-ID im POST → 404 wie eine ungültige UUID in der Adresse (Muster der bestehenden Resources)
- Risiko: zwei gleichzeitige POSTs derselben Abhängigkeit → der zweite scheitert am Primärschlüssel (500 statt 400);
  eine gleichzeitige direkte Umkehr kann durchgehen. In M1 hingenommen
- `node_modules` fehlte im Worktree; `npm ci` (kein Install neuer Pakete) nachgeholt
- Der Backend-`test-author` durfte Maven nicht ausführen; das Rot (UnsupportedOperationException) habe ich danach
  selbst belegt, das Frontend-Rot ebenfalls (33 rote US-16-Tests, bestehende grün)
