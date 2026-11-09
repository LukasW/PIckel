# Präsentationen für die AI Champions

Zwei Präsentationen im Erscheinungsbild der CSS Versicherung, jede eine einzige HTML-Datei ohne externe
Abhängigkeiten; im Browser öffnen.

| Datei | Inhalt |
|-------|--------|
| `index.html` | «AI Champions: Wohin wir gehen» — Zukunft (Spec Driven, Dark Factory), Schritt 1: Spec Driven, Zum Üben (eigenes Harness, PIckel, Einrichtung), Rolle der Champions, Abgrenzung zur AI Factory |
| `flow.html` | «Der Flow-Prozess und seine Elemente» — Vertiefung: Elemente (Bausteine, Kontext, Hooks, Leitplanken, Testpyramide), Ablauf vom Issue bis zum PR, Alltag (Demo, Gewohnheiten, Stolpersteine) |

## Bedienung

| Taste | Wirkung |
|-------|---------|
| → Leertaste Enter Bild↓ | nächste Folie |
| ← Bild↑ | vorherige Folie |
| Pos1 / Ende | erste / letzte Folie |
| N | Sprechernotizen ein- und ausblenden |
| F | Vollbild |
| P | Drucken; als PDF speichern ergibt eine Seite pro Folie |

Die Adresse merkt sich die Folie (`flow.html#12`), ein Neuladen bleibt also auf derselben Seite.

## Pflege

- Jede Folie ist ein `<section class="slide">`, die Sprechernotizen stehen im `<aside class="notes">` darin.
  Fusszeile und Seitenzahl setzt das Script am Ende der Datei
- Die Fakten stammen aus `CLAUDE.md`, `claude-guide.md`, `.claude/flow.md`, `.claude/settings.json`, `.devcontainer/`,
  `spec/fachliche-spezifikation.md`, `spec/user-stories/` (Stand der Releases), `docs/adr/` und
  `.agents/skills/flow-implement/SKILL.md`. Ändert sich der Ablauf oder der fachliche Stand dort, die Folien
  nachführen
- Farben, Raster und Logo folgen der CSS-Präsentationsvorlage: Navy `#00327D`, Cyan `#00A4E0`, Markenverlauf nur
  auf Titel-, Kapitel- und Statementfolien, eckige Flächen ohne Rahmen, Arial
- Beide Dateien tragen dieselben Styles und dasselbe Script; wer dort etwas ändert, ändert es in beiden
