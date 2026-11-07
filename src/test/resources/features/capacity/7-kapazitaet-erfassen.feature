# language: de
Funktionalität: US-7 Kapazität erfassen

  Als RTE
  möchte ich die Kapazität jedes Teams pro Iteration eines PI in Story Points erfassen
  damit die Planung gegen die verfügbare Kapazität geprüft werden kann

  Szenario: Kapazität wird erfasst
    Gegeben sei eine leere Liste der PIs
    Und eine leere Teamliste
    Und das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und das Team "Rakete"
    Wenn ich für "Rakete" in "Iteration 1" von "PI 27.1" die Kapazität 40 erfasse
    Dann hat "Rakete" in "Iteration 1" von "PI 27.1" die Kapazität 40

  Szenario: Ohne Eintrag ist die Kapazität 0
    Gegeben sei eine leere Liste der PIs
    Und eine leere Teamliste
    Und das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und das Team "Rakete"
    Dann hat "Rakete" in "Iteration 2" von "PI 27.1" die Kapazität 0

  Szenario: Negative Kapazität wird abgelehnt
    Gegeben sei eine leere Liste der PIs
    Und eine leere Teamliste
    Und das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und das Team "Rakete"
    Wenn ich für "Rakete" in "Iteration 1" von "PI 27.1" die Kapazität -5 erfasse
    Dann wird das Erfassen mit der Meldung "Die Kapazität muss eine ganze Zahl ab 0 sein." abgelehnt

  Szenario: Kapazität für die IP-Iteration wird abgelehnt
    Gegeben sei eine leere Liste der PIs
    Und eine leere Teamliste
    Und das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und das Team "Rakete"
    Wenn ich für "Rakete" in "IP" von "PI 27.1" die Kapazität 10 erfasse
    Dann wird das Erfassen mit der Meldung "In der IP-Iteration wird keine Kapazität geplant." abgelehnt

  Szenario: Kapazität ist pro PI getrennt
    Gegeben sei eine leere Liste der PIs
    Und eine leere Teamliste
    Und das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und das PI "PI 27.2" vom "2027-04-05" bis "2027-06-25" mit Iterationen zu 2 Wochen
    Und das Team "Rakete"
    Wenn ich für "Rakete" in "Iteration 1" von "PI 27.1" die Kapazität 40 erfasse
    Dann hat "Rakete" in "Iteration 1" von "PI 27.2" die Kapazität 0

  Szenario: Kapazität mit Nachkommastellen wird abgelehnt
    Gegeben sei eine leere Liste der PIs
    Und eine leere Teamliste
    Und das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und das Team "Rakete"
    Wenn ich für "Rakete" in "Iteration 1" von "PI 27.1" die Kapazität 1.5 erfasse
    Dann wird das Erfassen mit der Meldung "Die Anfrage enthält einen ungültigen Wert." abgelehnt

  Szenario: Kapazität für ein unbekanntes Team wird abgelehnt
    Gegeben sei eine leere Liste der PIs
    Und eine leere Teamliste
    Und das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Wenn ich für ein unbekanntes Team in "Iteration 1" von "PI 27.1" die Kapazität 40 erfasse
    Dann wird die Änderung mit dem Status 404 und der Meldung "Das Team gibt es nicht." abgelehnt

  Szenario: Kapazität in einem unbekannten PI wird abgelehnt
    Gegeben sei eine leere Teamliste
    Und das Team "Rakete"
    Wenn ich für "Rakete" in der ersten Iteration eines unbekannten PI die Kapazität 40 erfasse
    Dann wird die Änderung mit dem Status 404 und der Meldung "Das PI gibt es nicht." abgelehnt

  # Vitest: capacity-table.component.spec.ts → zeigt ohne Teams einen Hinweis mit Link zur Teamseite (US-7)
  @Unit
  Szenario: Ohne Teams zeigt die Tabelle einen Hinweis
    Gegeben sei ein PI ohne Teams
    Wenn ich die Detailseite öffne
    Dann sehe ich einen Hinweis mit Link zur Teamseite
