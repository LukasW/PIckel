# Notizen zu Issue #50

## PR 1 (fix/50-ip-drop)

- Methode im `BoardService` heisst `rejectInnovationAndPlanning()` und kennt den Text selbst: Die Meldung ist eine
  fachliche Regel (AK 3), sie gehört in den Use Case und nicht in die Board-Komponente
- Die vollständige `delta.md` mit den Abschnitten für PR 2 liegt ungetrackt als `delta-pr2-entwurf.md` im
  Change-Ordner; PR 1 committet `delta.md` nur mit dem Abschnitt US-13 (Plan → «Zwei PRs, ein Delta»)
- Annahme (bestätigt bei der Freigabe, 2026-11-08): Neben dem Raster losgelassen bleibt eine frühere Meldung stehen;
  über der IP ersetzt die IP-Meldung sie. Der Kopf der IP-Spalte zählt nicht als «über der IP», nur die Zellen
- Echtes Ziehen mit der Maus prüft kein Test (jsdom); Prüfung von Hand im Browser steht aus
