# language: de
Funktionalität: US-8 Business-Kontext und ART-Ziele am PI

  Als RTE
  möchte ich den Business-Kontext und die PI Objectives des ART als Text und Links am PI hinterlegen
  damit alle Teams im Planning dieselbe Ausgangslage sehen

  Szenario: Business-Kontext wird erfasst
    Gegeben sei eine leere Liste der PIs
    Und das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich für "PI 27.1" den Business-Kontext "Fokus auf Self-Service" erfasse
    Dann zeigt "PI 27.1" den Business-Kontext "Fokus auf Self-Service"

  Szenario: Link wird hinzugefügt
    Gegeben sei eine leere Liste der PIs
    Und das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich für "PI 27.1" den Link "Vision" mit der URL "https://wiki.example.ch/vision" hinzufüge
    Dann zeigt "PI 27.1" den Link "Vision"

  Szenario: Business-Kontext wird geändert
    Gegeben sei eine leere Liste der PIs
    Und das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Und für "PI 27.1" ist der Business-Kontext "Fokus auf Self-Service" erfasst
    Wenn ich für "PI 27.1" den Business-Kontext "Fokus auf Onboarding" erfasse
    Dann zeigt "PI 27.1" den Business-Kontext "Fokus auf Onboarding"

  Szenario: Link wird entfernt
    Gegeben sei eine leere Liste der PIs
    Und das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Und für "PI 27.1" sind die Links "Vision" und "Roadmap" erfasst
    Wenn ich für "PI 27.1" den Link "Vision" entferne
    Dann zeigt "PI 27.1" nur noch den Link "Roadmap"

  Szenario: Link ohne http oder https wird abgelehnt
    Gegeben sei eine leere Liste der PIs
    Und das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich für "PI 27.1" den Link "Vision" mit der URL "javascript:alert(1)" hinzufüge
    Dann wird das Hinzufügen mit der Meldung "Die URL muss mit http:// oder https:// beginnen." abgelehnt

  # JUnit: domain/model/ProgramIncrementTest → rejectsBusinessContextLongerThan10000Characters
  @Unit
  Szenario: Zu langer Business-Kontext wird abgelehnt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
    Wenn ich für "PI 27.1" einen Business-Kontext mit 10 001 Zeichen erfasse
    Dann wird das Erfassen mit der Meldung "Der Text darf höchstens 10 000 Zeichen lang sein." abgelehnt

  # Vitest: src/main/webapp/src/app/features/program-increment-detail/program-increment-detail.component.spec.ts → ändert den Business-Kontext über «Bearbeiten» (US-8)
  # Vitest: src/main/webapp/src/app/features/program-increment-detail/program-increment-detail.component.spec.ts → schliesst beim Abbrechen das Formular und verwirft die Eingabe (US-8)
  @Unit
  Szenario: Business-Kontext wird über Bearbeiten geändert
    Gegeben sei ein PI mit dem Business-Kontext "Digitalisierung"
    Wenn ich die Detailseite öffne
    Dann steht "Digitalisierung" einmal auf der Seite und kein Eingabefeld dafür
    Wenn ich "Bearbeiten" wähle, den Text ändere und speichere
    Dann zeigt die Seite den neuen Text und das Formular ist wieder zu
