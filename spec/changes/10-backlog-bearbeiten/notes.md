# Notizen #10

- **Annahme: PUT ersetzt das ganze Feature.** Eine fehlende optionale Angabe (Beschreibung, Rang, Owner, Schätzung)
  leert sie, wie beim Erfassen. Grund: HTTP-PUT-Semantik, derselbe `FeatureRequest` wie beim POST, und das Formular
  sendet immer den ganzen Stand. Belegt in `FeatureServiceTest → updateReplacesStoredValuesWithSentOnes` und
  `FeatureTest → reviseWithoutOptionalValuesClearsThem`
- **Annahme: Feature eines anderen PI ist unter diesem PI unbekannt (404).** Sonst liesse sich jedes Feature über eine
  beliebige PI-Adresse ändern. Delta um AK 6 und drei Szenarien ergänzt (nur «Neu»)
- **Annahme: Bearbeiten nutzt das Erfassungsformular** (Absende-Button «Speichern», dazu «Abbrechen»), nach dem
  Speichern zurück zum Erfassen. Hält «dieselben Regeln wie beim Erfassen» an einem Ort
- **Annahme: Bestätigung inline** in der Zeile («Wirklich löschen» / «Abbrechen») wie beim Entfernen von Teams (US-6),
  kein Dialog. Szenario «Löschen verlangt eine Bestätigung» als `@Unit` im Delta nachgetragen
- **Leerzustand:** Der Text ändert sich gegenüber dem Stand von US-9 («… Erfasse das erste Feature.»). US-9 spezifiziert
  den Leerzustand nicht (weder Story noch Feature), deshalb «Neu» und nicht «Geändert»; der US-9-Vitest prüft nur noch
  den Anfang «Noch keine Features.», den genauen Text der US-10-Test
- **Sortierung** war mit US-9 schon umgesetzt; die beiden Sortier-Szenarien von US-10 belegen bestehendes Verhalten
- `FeaturesService#add` nutzt jetzt dieselbe Hilfsmethode wie `update`/`remove` (Ändern, neu laden, Meldung setzen)
