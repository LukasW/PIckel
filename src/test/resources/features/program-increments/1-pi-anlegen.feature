# language: de
Funktionalität: US-1 PI anlegen

  Als RTE
  möchte ich ein Program Increment mit Name und Zeitraum anlegen
  damit das Team dafür planen kann

  Szenario: PI wird angelegt und erscheint in der Liste
    Wenn ich das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" anlege
    Dann wird das PI mit einer ID angelegt
    Und erscheint "PI 27.1" in der Liste der PIs

  Szenario: Enddatum vor Startdatum wird abgelehnt
    Wenn ich das PI "PI 27.2" vom "2027-03-26" bis "2027-01-04" anlege
    Dann wird das Anlegen mit der Meldung "Das Enddatum muss nach dem Startdatum liegen." abgelehnt

  # JUnit: domain/model/ProgramIncrementTest → rejectsBlankName
  @Unit
  Szenario: Leerer Name wird abgelehnt
    Wenn ich das PI " " vom "2027-01-04" bis "2027-03-26" anlege
    Dann wird das Anlegen mit der Meldung "Der Name darf nicht leer sein." abgelehnt
