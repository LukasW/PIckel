# Entscheide — Issue #13

## Geklärte Entscheidungen
- **Einplanung:** Wert am Feature (Team und Iteration), höchstens eine Zelle (ADR 0005, ADR 0006)
- **IP-Iteration:** gesperrt; Server lehnt ab, Oberfläche bietet sie nicht an (Delta, ADR 0006)
- **Drag & Drop:** Angular CDK (Begründung: Touch am Wandbildschirm, weniger eigener Code, testbar). `@angular/cdk`
  kommt vor dem Lauf von Hand per `chore/`-PR auf `main`, weil `package.json` laut `flow.md` nie in einen
  Nachtlauf gehört
- **Bedienung ohne Maus:** Aktion «Einplanen …» als Inline-Formular an der Karte mit nativen Auswahlfeldern (Team,
  Iteration ohne IP) und «Zurück ins Backlog» bei eingeplanten Karten (Begründung: kein Fokusmanagement eines
  Dialogs, passt zur Inline-Bestätigung bei den Teams)
- **Team entfernen:** Eingeplante Features gehen in allen PIs zurück ins Backlog. Die Bestätigung weist mit festem
  Text darauf hin (Begründung: Planung geht nicht unbemerkt verloren, ohne neuen Endpunkt)

## Bewusst draussen (Non-Goals)
- Zählung der betroffenen Features im Hinweis beim Entfernen eines Teams (mögliche Folge-Story nach M1)
- Gleichzeitiges Arbeiten mehrerer Personen (M2), Reihenfolge innerhalb einer Zelle
- Kapazitätsprüfung beim Einplanen: Überlast wird in #14 angezeigt, nicht verhindert

## Verbleibende Annahmen
- Bei der Umsetzung festzulegen, im Plan zu bestätigen: Endpunkt zum Einplanen und Zurücknehmen am Feature, das
  Board liefert die Einplanung pro Zelle mit (Grundlage für #14 und #17), das Zurückgeben beim Entfernen eines Teams
  über einen Fremdschlüssel (`ON DELETE SET NULL`, wie bei der Kapazität `CASCADE`) in einer neuen Migration, die
  keine Daten ändert
- Innerhalb einer Zelle gilt die Reihenfolge des Backlogs (WSJF-Rang)

## Auswirkung auf die Spezifikation
- Delta US-13: Entscheide nachgeführt, ein Akzeptanzkriterium zum Hinweis beim Entfernen eines Teams ergänzt
- Team-Story #6: ihre Bestätigung bekommt den Hinweistext (im Delta unter «Team entfernen» festgehalten)
- Keine neue ADR
