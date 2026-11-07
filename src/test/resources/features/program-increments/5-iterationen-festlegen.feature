# language: de
Funktionalität: US-5 Iterationen festlegen

  Als RTE
  möchte ich beim Anlegen eines PI die Iterationslänge wählen und die Iterationen samt IP-Iteration sehen
  damit das Program Board die Spalten des PI kennt

  Szenario: Iterationen werden aus der Iterationslänge erzeugt
    Gegeben sei eine leere Liste der PIs
    Wenn ich das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen anlege
    Dann hat das PI 6 Iterationen
    Und beginnt "Iteration 1" am "2027-01-04" und endet am "2027-01-17"
    Und beginnt "IP" am "2027-03-15" und endet am "2027-03-26"

  Szenario: Iterationen folgen der gewählten Länge
    Gegeben sei eine leere Liste der PIs
    Wenn ich das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 3 Wochen anlege
    Dann hat das PI 4 Iterationen
    Und beginnt "IP" am "2027-03-08" und endet am "2027-03-26"

  Szenario: Ohne Iterationslänge gelten 2 Wochen
    Wenn ich das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" ohne Iterationslänge anlege
    Dann hat das PI 6 Iterationen

  # JUnit: domain/model/ProgramIncrementTest → lastIterationIsShorterWhenPeriodHasRemainder
  @Unit
  Szenario: Letzte Iteration ist bei einem Rest kürzer
    Wenn ich das PI "PI 27.1" vom "2027-01-04" bis "2027-02-07" mit Iterationen zu 2 Wochen anlege
    Dann hat das PI 3 Iterationen
    Und beginnt "IP" am "2027-02-01" und endet am "2027-02-07"

  # JUnit: domain/model/ProgramIncrementTest → rejectsIterationLengthOutsideOneToFourWeeks
  @Unit
  Szenario: Iterationslänge ausserhalb von 1 bis 4 Wochen wird abgelehnt
    Wenn ich das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 5 Wochen anlege
    Dann wird das Anlegen mit der Meldung "Die Iterationslänge muss zwischen 1 und 4 Wochen liegen." abgelehnt

  # JUnit: domain/model/ProgramIncrementTest → rejectsPeriodShorterThanTwoIterations
  @Unit
  Szenario: Zeitraum für weniger als zwei Iterationen wird abgelehnt
    Wenn ich das PI "PI 27.1" vom "2027-01-04" bis "2027-01-10" mit Iterationen zu 2 Wochen anlege
    Dann wird das Anlegen mit der Meldung "Der Zeitraum reicht nicht für mindestens zwei Iterationen." abgelehnt

  Szenario: Unbekanntes PI wird nicht gefunden
    Wenn ich ein PI mit einer unbekannten ID abfrage
    Dann wird die Abfrage mit dem Status 404 und der Meldung "Das PI gibt es nicht." abgelehnt

  Szenario: PI mit ungültiger ID wird nicht gefunden
    Wenn ich das PI mit der ID "keine-uuid" abfrage
    Dann wird die Abfrage mit dem Status 404 und der Meldung "Das PI gibt es nicht." abgelehnt

  # Vitest: program-increment-detail.component.spec.ts → zeigt die Iterationen mit markierter IP-Iteration (US-5)
  @Unit
  Szenario: Detailseite zeigt die Iterationen mit markierter IP-Iteration
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Wenn ich die Detailseite von "PI 27.1" öffne
    Dann sehe ich 6 Iterationen mit Start- und Enddatum im Format TT.MM.JJJJ
    Und ist "IP" als IP-Iteration markiert
