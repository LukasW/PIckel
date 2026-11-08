# language: de
Funktionalität: US-16 Abhängigkeit zwischen Features erfassen

  Als Teilnehmerin oder Teilnehmer des PI Planning
  möchte ich festhalten, dass ein Feature ein anderes Feature desselben PI braucht
  damit Abhängigkeiten zwischen Teams im Planning erfasst sind und nicht im Kopf einzelner Personen

  # Die Regeln prüft FeatureDependencyTest, die Anzeige in beide Richtungen die Vitest-Specs; hier der REST-Vertrag.
  Szenario: Abhängigkeit wird erfasst
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Und im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
    Und im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
    Wenn ich festhalte, dass "Portal" das Feature "Login" braucht
    Dann braucht "Portal" das Feature "Login"
    Und wird "Login" von "Portal" gebraucht

  Szenario: Feature, das sich selbst braucht, wird abgelehnt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Und im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
    Wenn ich festhalte, dass "Portal" das Feature "Portal" braucht
    Dann wird das Erfassen mit der Meldung "Ein Feature kann nicht von sich selbst abhängen." abgelehnt

  Szenario: Doppelte Abhängigkeit wird abgelehnt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Und im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
    Und im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
    Und "Portal" braucht "Login"
    Wenn ich festhalte, dass "Portal" das Feature "Login" braucht
    Dann wird das Erfassen mit der Meldung "Diese Abhängigkeit gibt es schon." abgelehnt

  Szenario: Umgekehrte Abhängigkeit wird abgelehnt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Und im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
    Und im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
    Und "Portal" braucht "Login"
    Wenn ich festhalte, dass "Login" das Feature "Portal" braucht
    Dann wird das Erfassen mit der Meldung "Die beiden Features hängen schon umgekehrt voneinander ab." abgelehnt

  Szenario: Abhängigkeit zu einem Feature aus einem anderen PI wird abgelehnt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Und das PI "PI 27.2" vom "2027-04-05" bis "2027-06-25"
    Und im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
    Und im PI "PI 27.2" das Feature "Login" ohne WSJF-Rang
    Wenn ich festhalte, dass "Portal" das Feature "Login" braucht
    Dann wird das Erfassen mit der Meldung "Beide Features müssen zum selben PI gehören." abgelehnt

  Szenario: Abhängigkeit wird entfernt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Und im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
    Und im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
    Und "Portal" braucht "Login"
    Wenn ich die Abhängigkeit von "Portal" zu "Login" entferne
    Dann braucht "Portal" kein Feature

  Szenario: Gelöschtes Feature nimmt seine Abhängigkeiten mit
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Und im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
    Und im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
    Und "Portal" braucht "Login"
    Wenn ich das Feature "Login" lösche
    Dann braucht "Portal" kein Feature

  Szenario: Gelöschtes brauchendes Feature nimmt seine Abhängigkeiten mit
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Und im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
    Und im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
    Und "Portal" braucht "Login"
    Wenn ich das Feature "Portal" lösche
    Dann wird "Login" von keinem Feature gebraucht

  # Vitest: src/main/webapp/src/app/features/feature-backlog/feature-backlog.component.spec.ts → bietet in der Auswahl nur Features an, mit denen das Feature in keiner Richtung verbunden ist (US-16)
  # Vitest: src/main/webapp/src/app/features/feature-backlog/feature-backlog.component.spec.ts → zeigt ohne Wahl den Platzhalter «Feature wählen …», «Abhängigkeit hinzufügen» ist nicht aktiv (US-16)
  # Vitest: src/main/webapp/src/app/features/feature-backlog/feature-backlog.component.spec.ts → sperrt «Abhängigkeit hinzufügen» wieder, wenn das gewählte Feature aus der Auswahl fällt (US-16)
  @Unit
  Szenario: Auswahl bietet nur noch nicht verbundene Features an
    Gegeben sei das Feature "Shop", das "Login" braucht, und das Feature "Suche"
    Wenn ich bei "Shop" die Auswahl "Braucht Feature" öffne
    Dann steht dort "Suche", aber weder "Login" noch "Shop"
    Und ohne Wahl zeigt sie "Feature wählen …" und "Abhängigkeit hinzufügen" ist nicht aktiv

  # Vitest: src/main/webapp/src/app/features/feature-backlog/feature-backlog.component.spec.ts → zeigt eine abgelehnte Abhängigkeit in der Zeile des Features, nicht über der Liste (US-16)
  # Vitest: src/main/webapp/src/app/features/feature-backlog/feature-backlog.component.spec.ts → lässt die Meldung verschwinden, sobald in einer anderen Zeile eine Abhängigkeit erfasst wird (US-16)
  # Vitest: src/main/webapp/src/app/features/feature-backlog/feature-backlog.component.spec.ts → lässt die Meldung verschwinden, sobald in einer anderen Zeile eine Abhängigkeit entfernt wird (US-16)
  # Vitest: src/main/webapp/src/app/features/feature-backlog/feature-backlog.component.spec.ts → ersetzt die Meldung, wenn die nächste Aktion in einer anderen Zeile ebenfalls scheitert (US-16)
  # Vitest: src/main/webapp/src/app/features/feature-backlog/feature-backlog.component.spec.ts → lässt die Meldung eines gescheiterten Entfernens bei der nächsten Aktion verschwinden (US-16)
  @Unit
  Szenario: Abgelehnte Abhängigkeit meldet sich beim Feature
    Gegeben sei das Backlog mit den Features "Shop" und "Suche"
    Wenn der Server die Abhängigkeit von "Shop" zu "Suche" ablehnt
    Dann steht seine Meldung in der Zeile von "Shop" und nicht über der Liste
    Und sie verschwindet, sobald ich die nächste Abhängigkeit erfasse oder entferne
