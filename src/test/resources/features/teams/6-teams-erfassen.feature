# language: de
Funktionalität: US-6 Teams erfassen

  Als RTE
  möchte ich die Teams des ART erfassen, umbenennen und entfernen
  damit sie in jedem PI für die Planung bereitstehen

  Szenario: Team wird erfasst und erscheint in der Liste
    Gegeben sei eine leere Teamliste
    Wenn ich das Team "Rakete" erfasse
    Dann erscheint "Rakete" in der Teamliste

  Szenario: Teamliste ist alphabetisch sortiert
    Gegeben sei eine leere Teamliste
    Und das Team "Zebra"
    Und das Team "Adler"
    Dann zeigt die Teamliste zuerst "Adler" und dann "Zebra"

  # Ohne @Unit: belegt auch den REST-Vertrag (400 mit Meldung); die Regel selbst prüft TeamTest.
  Szenario: Leerer Teamname wird abgelehnt
    Wenn ich das Team " " erfasse
    Dann wird das Erfassen mit der Meldung "Der Teamname darf nicht leer sein." abgelehnt

  Szenario: Zu langer Teamname wird abgelehnt
    Wenn ich ein Team mit 51 Zeichen im Namen erfasse
    Dann wird das Erfassen mit der Meldung "Der Teamname darf höchstens 50 Zeichen lang sein." abgelehnt

  Szenario: Doppelter Teamname wird abgelehnt
    Gegeben sei eine leere Teamliste
    Und das Team "Rakete"
    Wenn ich das Team "rakete" erfasse
    Dann wird das Erfassen mit der Meldung "Ein Team mit diesem Namen gibt es schon." abgelehnt

  Szenario: Team wird umbenannt
    Gegeben sei eine leere Teamliste
    Und das Team "Rakete"
    Wenn ich das Team "Rakete" in "Komet" umbenenne
    Dann erscheint "Komet" in der Teamliste
    Und erscheint "Rakete" nicht mehr in der Teamliste

  Szenario: Team wird entfernt
    Gegeben sei eine leere Teamliste
    Und das Team "Rakete"
    Wenn ich das Team "Rakete" entferne
    Dann ist die Teamliste leer

  Szenario: Umbenennen lässt die anderen Teams unverändert
    Gegeben sei eine leere Teamliste
    Und das Team "Rakete"
    Und das Team "Zebra"
    Wenn ich das Team "Rakete" in "Komet" umbenenne
    Dann zeigt die Teamliste zuerst "Komet" und dann "Zebra"

  Szenario: Entfernen lässt die anderen Teams stehen
    Gegeben sei eine leere Teamliste
    Und das Team "Rakete"
    Und das Team "Zebra"
    Wenn ich das Team "Rakete" entferne
    Dann erscheint "Zebra" in der Teamliste
    Und erscheint "Rakete" nicht mehr in der Teamliste

  # REST-Vertrag (notes.md): PUT antwortet 200 mit dem Team, DELETE 204; unbekannte oder ungültige ID → 404.
  # Nicht im Delta; belegt die Antworten, auf die sich das Frontend verlässt (wie US-5 «Unbekanntes PI»).
  Szenario: Umbenennen antwortet mit dem umbenannten Team
    Gegeben sei eine leere Teamliste
    Und das Team "Rakete"
    Wenn ich das Team "Rakete" in " Komet " umbenenne
    Dann antwortet das Umbenennen mit dem Team "Komet"

  Szenario: Umbenennen auf den Namen eines anderen Teams wird abgelehnt
    Gegeben sei eine leere Teamliste
    Und das Team "Rakete"
    Und das Team "Komet"
    Wenn ich das Team "Rakete" in "KOMET" umbenenne
    Dann wird die Änderung mit dem Status 400 und der Meldung "Ein Team mit diesem Namen gibt es schon." abgelehnt

  Szenario: Entfernen antwortet ohne Inhalt
    Gegeben sei eine leere Teamliste
    Und das Team "Rakete"
    Wenn ich das Team "Rakete" entferne
    Dann antwortet das Entfernen ohne Inhalt

  Szenariogrundriss: Unbekanntes Team lässt sich nicht umbenennen
    Wenn ich das Team mit der ID "<id>" in "Komet" umbenenne
    Dann wird die Änderung mit dem Status 404 und der Meldung "Das Team gibt es nicht." abgelehnt

    Beispiele:
      | id                                   |
      | 6f1c2b7e-0d4a-4c1e-9b7a-2f3e4d5c6b7a |
      | keine-uuid                           |

  Szenariogrundriss: Unbekanntes Team lässt sich nicht entfernen
    Wenn ich das Team mit der ID "<id>" entferne
    Dann wird die Änderung mit dem Status 404 und der Meldung "Das Team gibt es nicht." abgelehnt

    Beispiele:
      | id                                   |
      | 6f1c2b7e-0d4a-4c1e-9b7a-2f3e4d5c6b7a |
      | keine-uuid                           |

  # Vitest: teams.component.spec.ts → entfernt ein Team erst nach der Bestätigung (US-6)
  @Unit
  Szenario: Entfernen verlangt eine Bestätigung
    Gegeben sei das Team "Rakete"
    Wenn ich das Team "Rakete" entfernen will
    Dann bleibt "Rakete" in der Teamliste, bis ich das Entfernen bestätige

  # Vitest: teams.component.spec.ts → zeigt den Leerzustand ohne Teams (US-6)
  @Unit
  Szenario: Leerzustand ohne Teams
    Gegeben sei eine leere Teamliste
    Wenn ich die Teamseite öffne
    Dann sehe ich den Hinweis "Noch keine Teams. Erfasse das erste Team."
