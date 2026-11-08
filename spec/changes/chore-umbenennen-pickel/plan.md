# Chore — Projekt in PIckel umbenennen

Delta: keines — nur der Name ändert sich (Oberfläche, Java-Paket, Build, Doku), kein fachliches Verhalten.

## Aufgaben

- [x] Oberfläche, OpenAPI-Titel, Gherkin-Szenario auf «PIckel»
- [x] Java-Paket `ch.grudligstrasse.pickel`, PIT-Konfiguration, `artifactId`, Angular-Projekt, Quinoa-Build-Verzeichnis
- [x] Image-Name, Log- und Cache-Pfade der Skripte, Doku und Ablauf
- [x] GitHub-URLs auf `LukasW/PIckel` nach der Umbenennung des Repositorys
- [x] Story-Dateien, abgeschlossene Change-Ordner und ADRs — nur Name und Links, keine fachliche Änderung

## Bewusst unverändert

- `author="pikel"` in `changeLog.xml` — der Autor gehört zur Identität eines ChangeSets; ein neuer Autor liesse
  Liquibase alle ChangeSets erneut ausführen
- Devcontainer-Volumes `pikel-*` — neue Namen wären leer (Claude-Konfiguration, gh-Anmeldung, `node_modules`)
