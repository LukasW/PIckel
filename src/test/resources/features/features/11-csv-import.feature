# language: de
Funktionalität: US-11 Features per CSV importieren

  Als Product Manager
  möchte ich die Features aus dem Issue-Tracker als CSV-Datei ins Backlog eines PI importieren
  damit ich sie vor dem Planning nicht abtippen muss

  # Die Regeln des Formats (Trennzeichen, RFC 4180, BOM, Zeilennummern, Grenzen) prüft FeatureCsvTest.
  Szenario: CSV mit Semikolon wird importiert
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich ins PI "PI 27.1" diese CSV importiere:
      """
      Titel;WSJF-Rang;Owner;Schätzung
      Portal;1;Anna;21
      Suche;2;Beat;13
      """
    Dann wurden 2 Features importiert
    Und zeigt das Backlog von "PI 27.1" die Features "Portal", "Suche" in dieser Reihenfolge

  Szenario: CSV mit Komma und Anführungszeichen wird importiert
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich ins PI "PI 27.1" diese CSV importiere:
      """
      Titel,Beschreibung
      "Export, CSV","Für den Issue-Tracker"
      """
    Dann wurde 1 Feature importiert
    Und erscheint "Export, CSV" im Backlog von "PI 27.1"

  Szenario: Ungültige Zeile verhindert den ganzen Import
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich ins PI "PI 27.1" diese CSV importiere:
      """
      Titel;WSJF-Rang
      Portal;1
      Suche;null
      """
    Dann wird der Import mit der Meldung "Zeile 3: Der WSJF-Rang muss eine ganze Zahl ab 1 sein." abgelehnt
    Und ist das Backlog von "PI 27.1" leer

  Szenario: CSV ohne Spalte Titel wird abgelehnt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich ins PI "PI 27.1" diese CSV importiere:
      """
      Name;Owner
      Portal;Anna
      """
    Dann wird der Import mit der Meldung "Die Kopfzeile braucht die Spalte «Titel»." abgelehnt

  # AK 5 über REST: Die Grenze greift im Use Case, nicht schon an einer Grenze des Servers.
  Szenario: CSV mit mehr als 1000 Datenzeilen wird abgelehnt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich ins PI "PI 27.1" eine CSV mit 1001 Datenzeilen importiere
    Dann wird der Import mit der Meldung "Die Datei darf höchstens 1000 Datenzeilen haben." abgelehnt
    Und ist das Backlog von "PI 27.1" leer

  Szenario: CSV über 1 MB wird abgelehnt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich ins PI "PI 27.1" eine CSV mit 1000001 Bytes importiere
    Dann wird der Import mit der Meldung "Die Datei darf höchstens 1 MB gross sein." abgelehnt
    Und ist das Backlog von "PI 27.1" leer

  Szenario: Import in ein unbekanntes PI wird abgelehnt
    Wenn ich in ein unbekanntes PI diese CSV importiere:
      """
      Titel
      Portal
      """
    Dann wird der Import mit dem Status 404 und der Meldung "Das PI gibt es nicht." abgelehnt

  # Vitest: feature-backlog.component.spec.ts → meldet nach dem Import die Anzahl und zeigt die Features im Backlog (US-11)
  # Vitest: feature-backlog.component.spec.ts → meldet einen einzelnen Import in der Einzahl (US-11)
  # Vitest: feature-backlog.component.spec.ts → zeigt die Meldung des Servers als Alarm und keine Anzahl, wenn er den Import ablehnt (US-11)
  @Unit
  Szenario: Oberfläche meldet die Anzahl der importierten Features
    Gegeben sei das Backlog eines PI
    Wenn ich eine CSV-Datei mit 2 Features zum Import wähle
    Dann meldet die Oberfläche "2 Features importiert."
    Und zeigt das Backlog die importierten Features

  # Vitest: src/main/webapp/src/app/features/feature-backlog/feature-backlog.component.spec.ts → nennt beim Import die Spalten der CSV-Datei, «Titel» als Pflicht (US-11)
  # Vitest: src/main/webapp/src/app/features/feature-backlog/feature-backlog.component.spec.ts → bietet eine Beispieldatei mit der Kopfzeile der Spalten zum Herunterladen an (US-11)
  @Unit
  Szenario: Oberfläche nennt das CSV-Format und bietet eine Beispieldatei
    Gegeben sei das Backlog eines PI
    Wenn ich den Import ansehe
    Dann nennt die Oberfläche die Spalten "Titel", "Beschreibung", "WSJF-Rang", "Owner" und "Schätzung"
    Und eine Beispieldatei mit dieser Kopfzeile lässt sich herunterladen
