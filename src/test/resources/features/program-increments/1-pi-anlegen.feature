# language: de
Funktionalität: US-1 PI anlegen

  Als RTE
  möchte ich ein Program Increment mit Name und Zeitraum anlegen
  damit das Team dafür planen kann

  Grundlage:
    Gegeben sei eine leere Liste der PIs

  Szenario: PI wird angelegt und erscheint in der Liste
    Wenn ich das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" anlege
    Dann wird das PI mit einer ID angelegt
    Und verweist die Antwort im Header `Location` auf das neue PI
    Und erscheint "PI 27.1" in der Liste der PIs

  Szenario: Liste ist aufsteigend nach Startdatum sortiert
    Gegeben sei das PI "PI 27.2" vom "2027-04-05" bis "2027-06-25"
    Und das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Dann zeigt die Liste der PIs zuerst "PI 27.1" und dann "PI 27.2"

  Szenario: Enddatum vor Startdatum wird abgelehnt
    Wenn ich das PI "PI 27.2" vom "2027-03-26" bis "2027-01-04" anlege
    Dann wird das Anlegen mit der Meldung "Das Enddatum muss nach dem Startdatum liegen." abgelehnt

  # JUnit: domain/model/ProgramIncrementTest → rejectsEndDateNotAfterStartDate
  @Unit
  Szenario: Enddatum gleich Startdatum wird abgelehnt
    Wenn ich das PI "PI 27.1" vom "2027-01-04" bis "2027-01-04" anlege
    Dann wird das Anlegen mit der Meldung "Das Enddatum muss nach dem Startdatum liegen." abgelehnt

  Szenario: Leerer Name wird abgelehnt
    Wenn ich das PI " " vom "2027-01-04" bis "2027-03-26" anlege
    Dann wird das Anlegen mit der Meldung "Der Name darf nicht leer sein." abgelehnt

  Szenario: Fehlendes Datum wird abgelehnt
    Wenn ich das PI "PI 27.1" ohne Enddatum anlege
    Dann wird das Anlegen mit der Meldung "Start- und Enddatum sind Pflicht." abgelehnt

  Szenario: Ungültiges Datum wird abgelehnt
    Wenn ich das PI "PI 27.1" vom "2027-01-04" bis "2027-13-45" anlege
    Dann wird das Anlegen mit der Meldung "Die Anfrage enthält einen ungültigen Wert." abgelehnt

  Szenario: Anfrage ohne gültiges JSON wird abgelehnt
    Wenn ich ein PI mit dem Inhalt "{ kein JSON" anlege
    Dann wird das Anlegen mit der Meldung "Die Anfrage ist kein gültiges JSON." abgelehnt

  Szenario: Anfrage ohne Inhalt wird abgelehnt
    Wenn ich ein PI ohne Inhalt anlege
    Dann wird das Anlegen mit der Meldung "Die Anfrage ist unvollständig." abgelehnt

  # Vitest: program-increments.component.spec.ts → zeigt einen Leerzustand ohne PIs (US-1)
  @Unit
  Szenario: Ohne PIs zeigt die Seite einen Leerzustand
    Wenn ich die Seite der PIs öffne
    Dann sehe ich "Noch keine PIs"

  # Vitest: program-increments.component.spec.ts → zeigt Pflichtfelder am Feld an, ohne anzulegen (US-1)
  @Unit
  Szenario: Formular zeigt fehlende Angaben am Feld an
    Wenn ich das Formular ohne Angaben absende
    Dann sehe ich am Namen "Der Name ist Pflicht."
    Und es wird kein PI angelegt

  # Vitest: program-increments.component.spec.ts → zeigt Daten im Format TT.MM.JJJJ (US-1)
  @Unit
  Szenario: Daten erscheinen im Schweizer Format
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich die Seite der PIs öffne
    Dann sehe ich "04.01.2027 – 26.03.2027"

  # Vitest: src/main/webapp/src/app/app.spec.ts → führt über «Program Increments» in der Kopfzeile zur Liste der PIs (US-1)
  # Vitest: src/main/webapp/src/app/app.spec.ts → zeigt den Link «Program Increments» auf jeder Seite, auch auf Detailseite und Board (US-1)
  # Vitest: src/main/webapp/src/app/app.spec.ts → verlinkt in der Kopfzeile «Program Increments» auf die PI-Übersicht, neben «Teams» (US-1)
  @Unit
  Szenario: Kopfzeile führt zur PI-Übersicht
    Gegeben sei eine beliebige Seite von PIkel
    Wenn ich in der Kopfzeile "Program Increments" wähle
    Dann sehe ich die Liste der PIs
