# language: de
Funktionalität: US-9 Feature im PI-Backlog erfassen

  Als Product Manager
  möchte ich Features im Backlog eines PI erfassen
  damit sie auf dem Program Board eingeplant werden können

  Szenario: Feature wird erfasst und erscheint im Backlog
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich im PI "PI 27.1" das Feature "Self-Service-Portal" mit WSJF-Rang 1, Owner "Anna" und Schätzung 21 erfasse
    Dann erscheint "Self-Service-Portal" im Backlog von "PI 27.1"
    Und ist "Self-Service-Portal" nicht eingeplant

  Szenario: Feature nur mit Titel wird erfasst
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich im PI "PI 27.1" das Feature "Login" ohne weitere Angaben erfasse
    Dann erscheint "Login" im Backlog von "PI 27.1"

  # Ohne @Unit: belegt auch den REST-Vertrag (400 mit Meldung); die Regeln selbst prüft FeatureTest.
  Szenario: Feature ohne Titel wird abgelehnt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich im PI "PI 27.1" das Feature " " erfasse
    Dann wird das Erfassen mit der Meldung "Der Titel darf nicht leer sein." abgelehnt

  Szenario: WSJF-Rang 0 wird abgelehnt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich im PI "PI 27.1" das Feature "Login" mit WSJF-Rang 0 erfasse
    Dann wird das Erfassen mit der Meldung "Der WSJF-Rang muss eine ganze Zahl ab 1 sein." abgelehnt

  Szenario: Negative Schätzung wird abgelehnt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich im PI "PI 27.1" das Feature "Login" mit Schätzung -1 erfasse
    Dann wird das Erfassen mit der Meldung "Die Schätzung muss eine ganze Zahl ab 0 sein." abgelehnt

  Szenario: Feature in einem unbekannten PI wird abgelehnt
    Wenn ich in einem unbekannten PI das Feature "Login" erfasse
    Dann antwortet der Server mit 404 und der Meldung "Das PI gibt es nicht."

  # AK 5: WSJF-Rang aufsteigend, ohne Rang am Ende, bei Gleichstand nach Titel ohne Gross-/Kleinschreibung.
  Szenario: Backlog ist nach WSJF-Rang und Titel sortiert
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich im PI "PI 27.1" das Feature "Suche" ohne weitere Angaben erfasse
    Und ich im PI "PI 27.1" das Feature "archiv" ohne weitere Angaben erfasse
    Und ich im PI "PI 27.1" das Feature "Bericht" mit WSJF-Rang 10 erfasse
    Und ich im PI "PI 27.1" das Feature "Zahlung" mit WSJF-Rang 2 erfasse
    Und ich im PI "PI 27.1" das Feature "login" mit WSJF-Rang 2 erfasse
    Und ich im PI "PI 27.1" das Feature "Export" mit WSJF-Rang 1 erfasse
    Dann zeigt das Backlog von "PI 27.1" die Features "Export, login, Zahlung, Bericht, archiv, Suche"

  # Vitest: feature-backlog.component.spec.ts → lehnt einen Titel aus Leerzeichen am Feld ab und sendet nichts (US-9)
  # Vitest: feature-backlog.component.spec.ts → lehnt den WSJF-Rang 0 am Feld ab und sendet nichts (US-9)
  # Vitest: feature-backlog.component.spec.ts → lehnt die Schätzung -1 am Feld ab und sendet nichts (US-9)
  @Unit
  Szenario: Formular zeigt ungültige Angaben am Feld und legt nichts an
    Gegeben sei das Backlog eines PI
    Wenn ich das Formular mit leerem Titel, WSJF-Rang 0 oder Schätzung -1 absende
    Dann steht die Meldung am betroffenen Feld
    Und es wird nichts angelegt
