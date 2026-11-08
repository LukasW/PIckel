# Notizen zu Issue #50

## PR 1 (fix/50-ip-drop)

- Methode im `BoardService` heisst `rejectInnovationAndPlanning()` und kennt den Text selbst: Die Meldung ist eine
  fachliche Regel (AK 3), sie gehört in den Use Case und nicht in die Board-Komponente
- Die vollständige `delta.md` mit den Abschnitten für PR 2 liegt ungetrackt als `delta-pr2-entwurf.md` im
  Change-Ordner; PR 1 committet `delta.md` nur mit dem Abschnitt US-13 (Plan → «Zwei PRs, ein Delta»)
- Annahme (bestätigt bei der Freigabe, 2026-11-08): Neben dem Raster losgelassen bleibt eine frühere Meldung stehen;
  über der IP ersetzt die IP-Meldung sie. Der Kopf der IP-Spalte zählt nicht als «über der IP», nur die Zellen
- Echtes Ziehen mit der Maus prüft kein Test (jsdom); Prüfung von Hand im Browser steht aus

## PR 2 (feat/50-bedienung)

- Kein Gerüst (6a): Alle Änderungen sind Templates und bestehende Komponenten, die Tests prüfen über das DOM
- `delta.md` enthält jetzt alle Abschnitte aus dem Entwurf; US-13 bleibt `eingepflegt` (PR 1)
- Annahme (klein): «Program Increments» steht in der Kopfzeile direkt vor «Teams»; der Link im leeren Backlog heisst
  «Zum PI», nach dem Muster «Noch keine Teams … Zu den Teams»
- Board-Kopf: `border-b border-border` am `thead` als Trennlinie; Prüfung von Hand (jsdom misst kein Layout)
- US-8: Absenden-Knopf des Link-Formulars heisst «Hinzufügen», damit «Link hinzufügen» eindeutig der Knopf zum Öffnen
  ist. `showWithContextForm` entfällt: Das Formular füllt sich erst beim Öffnen
- Annahme (klein, US-8): Sind nur Links erfasst, heisst der Knopf «Bearbeiten» (kein Hinweis, also kein «Erfassen»).
  Lehnt der Server das Speichern ab, bleibt das Formular mit der Eingabe offen. Beide Formulare dürfen zugleich offen
  sein (der bestehende Test «behält ungespeicherten Business-Kontext …» verlangt es)
- Ungeplant: `FeatureFieldsComponent` (`feature-backlog/feature-fields.component.*`) statt einer Zeilen-Komponente.
  Erfassen oben und Bearbeiten in der Zeile brauchen dieselben fünf Felder mit Meldungen; ohne eigene Komponente
  stünden sie zweimal im Template. `buildFeatureForm` liegt dort, damit beide Formulare dieselben Validatoren haben
- Spec-Review: Eine Wahl in «Braucht Feature», die nach einem Neuladen nicht mehr zur Auswahl steht, liess den Knopf
  aktiv. `neededChoice` lässt sie nur gelten, solange das Feature unter `candidatesFor` steht (eigener Test)
