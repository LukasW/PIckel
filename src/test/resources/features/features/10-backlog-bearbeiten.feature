# language: de
Funktionalität: US-10 Backlog nach WSJF-Rang bearbeiten

  Als Product Manager
  möchte ich das Backlog eines PI nach WSJF-Rang sehen und bearbeiten
  damit die priorisierte Liste bis zum Planning stimmt

  Szenario: Backlog ist nach WSJF-Rang sortiert, ohne Rang am Ende
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Und im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
    Und im PI "PI 27.1" das Feature "Suche" mit WSJF-Rang 2
    Und im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
    Dann zeigt das Backlog von "PI 27.1" die Features "Portal", "Suche", "Login" in dieser Reihenfolge

  Szenario: Gleicher Rang wird nach Titel sortiert
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Und im PI "PI 27.1" das Feature "Suche" mit WSJF-Rang 1
    Und im PI "PI 27.1" das Feature "Export" mit WSJF-Rang 1
    Dann zeigt das Backlog von "PI 27.1" die Features "Export", "Suche" in dieser Reihenfolge

  # Ohne @Unit: belegt den REST-Vertrag (PUT 200 mit dem Feature); die Regeln selbst prüft FeatureTest.
  Szenario: Feature wird bearbeitet
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Und im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
    Wenn ich beim Feature "Login" den WSJF-Rang auf 3 und die Schätzung auf 8 setze
    Dann hat "Login" den WSJF-Rang 3 und die Schätzung 8

  Szenario: Bearbeiten mit leerem Titel wird abgelehnt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Und im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
    Wenn ich beim Feature "Login" den Titel auf " " setze
    Dann wird das Bearbeiten mit der Meldung "Der Titel darf nicht leer sein." abgelehnt

  Szenario: Feature wird gelöscht
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Und im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
    Wenn ich das Feature "Login" lösche
    Dann ist das Backlog von "PI 27.1" leer

  # Vitest: feature-backlog.component.spec.ts → zeigt den Leerzustand mit Hinweis auf CSV-Import (US-10)
  @Unit
  Szenario: Leerzustand ohne Features
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich das Backlog von "PI 27.1" öffne
    Dann sehe ich den Hinweis "Noch keine Features. Erfasse das erste Feature oder importiere eine CSV-Datei."

  # Vitest: feature-backlog.component.spec.ts → löscht ein Feature erst nach der Bestätigung und zeigt dann den Leerzustand (US-10)
  # Vitest: feature-backlog.component.spec.ts → behält das Feature, wenn das Löschen abgebrochen wird (US-10)
  @Unit
  Szenario: Löschen verlangt eine Bestätigung
    Gegeben sei das Backlog eines PI mit dem Feature "Login"
    Wenn ich beim Feature "Login" auf «Löschen» klicke
    Dann wird noch nichts gelöscht, erst «Wirklich löschen» löscht das Feature; «Abbrechen» behält es

  # Unbekanntes oder fremdes Feature → 404 mit Meldung (wie US-6 «Unbekanntes Team»).
  Szenariogrundriss: Unbekanntes Feature lässt sich nicht bearbeiten
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich im PI "PI 27.1" das Feature mit der ID "<id>" bearbeite
    Dann antwortet der Server mit 404 und der Meldung "Das Feature gibt es nicht."

    Beispiele:
      | id                                   |
      | 6f1c2b7e-0d4a-4c1e-9b7a-2f3e4d5c6b7a |
      | keine-uuid                           |

  Szenariogrundriss: Unbekanntes Feature lässt sich nicht löschen
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich im PI "PI 27.1" das Feature mit der ID "<id>" lösche
    Dann antwortet der Server mit 404 und der Meldung "Das Feature gibt es nicht."

    Beispiele:
      | id                                   |
      | 6f1c2b7e-0d4a-4c1e-9b7a-2f3e4d5c6b7a |
      | keine-uuid                           |

  Szenario: Feature eines anderen PI lässt sich dort nicht bearbeiten
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Und das PI "PI 27.2" vom "2027-04-05" bis "2027-06-25"
    Und im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
    Wenn ich das Feature "Login" im PI "PI 27.2" bearbeite
    Dann antwortet der Server mit 404 und der Meldung "Das Feature gibt es nicht."
    Und erscheint "Login" weiterhin im Backlog von "PI 27.1"

  Szenario: Feature eines anderen PI lässt sich dort nicht löschen
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Und das PI "PI 27.2" vom "2027-04-05" bis "2027-06-25"
    Und im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
    Wenn ich das Feature "Login" im PI "PI 27.2" lösche
    Dann antwortet der Server mit 404 und der Meldung "Das Feature gibt es nicht."
    Und erscheint "Login" weiterhin im Backlog von "PI 27.1"

  # Vitest: src/main/webapp/src/app/features/feature-backlog/feature-backlog.component.spec.ts → macht beim Bearbeiten die Zeile des Features zum Formular, das Erfassen bleibt leer (US-10)
  # Vitest: src/main/webapp/src/app/features/feature-backlog/feature-backlog.component.spec.ts → zeigt nach dem Speichern die neuen Angaben in der Zeile des Features (US-10)
  # Vitest: src/main/webapp/src/app/features/feature-backlog/feature-backlog.component.spec.ts → schliesst beim Bearbeiten eines zweiten Features das erste ohne zu speichern (US-10)
  @Unit
  Szenario: Feature wird in seiner Zeile bearbeitet
    Gegeben sei das Backlog mit den Features "Login" und "Shop"
    Wenn ich bei "Shop" "Bearbeiten" wähle
    Dann wird die Zeile von "Shop" zum Formular mit seinen Angaben
    Und das Formular zum Erfassen bleibt leer
    Und nach "Speichern" zeigt die Zeile die neuen Angaben
