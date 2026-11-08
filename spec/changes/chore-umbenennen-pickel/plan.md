# Chore — Projekt in PIckel umbenennen

Delta: keines — nur der Name ändert sich (Oberfläche, Java-Paket, Build, Doku), kein fachliches Verhalten.

## Aufgaben

- [x] Oberfläche, OpenAPI-Titel, Gherkin-Szenario auf «PIckel»
- [x] Java-Paket `ch.grudligstrasse.pickel`, PIT-Konfiguration, `artifactId`, Angular-Projekt, Quinoa-Build-Verzeichnis
- [x] Image-Name, Log- und Cache-Pfade der Skripte, Doku und Ablauf
- [x] GitHub-URLs auf `LukasW/PIckel` nach der Umbenennung des Repositorys
- [x] Story-Dateien, abgeschlossene Change-Ordner und ADRs — nur Name und Links, keine fachliche Änderung

- [x] Liquibase-Autor `pickel` in `changeLog.xml` — bewusste Ausnahme von «bestehende Migrationen nie ändern»,
  weil noch keine dauerhafte Datenbank existiert (Dev Services und Tests starten leer)
- [x] Devcontainer-Volumes `pickel-*`

## Folgen für bestehende Umgebungen

- Keine Datenbank ist produktiv, darum ist kein Nachführen von `databasechangelog` nötig
- Die Volumes `pickel-*` sind beim nächsten Rebuild leer; Inhalt vorher von `pikel-*` kopieren.
