# Chore — Projekt in PIckel umbenennen

Delta: keines — nur der Name ändert sich (Oberfläche, Java-Paket, Build, Doku), kein fachliches Verhalten.

## Aufgaben

- [x] Oberfläche, OpenAPI-Titel, Gherkin-Szenario auf «PIckel»
- [x] Java-Paket `ch.grudligstrasse.pickel`, PIT-Konfiguration, `artifactId`, Angular-Projekt, Quinoa-Build-Verzeichnis
- [x] Image-Name, Log- und Cache-Pfade der Skripte, Doku und Ablauf
- [x] GitHub-URLs auf `LukasW/PIckel` nach der Umbenennung des Repositorys

## Bewusst unverändert

- `author="pikel"` in `changeLog.xml` — der Autor gehört zur Identität eines ChangeSets
- Story-Dateien, abgeschlossene Change-Ordner und ADRs — eine geänderte Story verlangt ein Delta; alte Links
  leitet GitHub weiter
- Devcontainer-Volumes `pikel-*` — neue Namen wären leer
