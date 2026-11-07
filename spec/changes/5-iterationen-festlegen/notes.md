# Notizen US-5

- Port-Methode heisst `findOne(UUID)` statt `findById`: `PanacheRepositoryBase.findById` kollidiert im Adapter mit dem Rückgabetyp `Optional`.
- Bestehende Dev-PIs kürzer als 15 Tage verletzen nach V2 die Mindestdauer. Weil der Adapter jedes PI über den prüfenden Konstruktor lädt, scheitert dann `GET /api/v1/program-increments` als Ganzes mit 400; Abhilfe: Dev-Datenbank neu aufsetzen. Akzeptiert, weil es ausser lokalen Dev-Daten keinen Bestand gibt (kein Deployment, Plan → «Offene Fragen»).
- Detailseite bekommt die ID über `withComponentInputBinding()` (neu in `app.config.ts`) als `input.required<string>()`, nicht über `ActivatedRoute`.
- IP-Zeile: Hintergrund `bg-background` plus Text-Badge «IP-Iteration»; es gibt kein eigenes Token für eine Hervorhebung, ein neues wäre spekulativ.
- Formular: Iterationslänge springt nach erfolgreichem Anlegen auf 2 zurück (wie die übrigen Felder; Annahme aus test-author).
- Szenario «Iterationen folgen der gewählten Länge» (3 Wochen) im Delta unter «Neu» ergänzt: Alle anderen ungetaggten Szenarien nutzen den Standard 2, ein Fehler im Speichern oder Mappen der Länge fiele sonst in `CucumberIT` nicht auf (Lücke aus test-author).
- Annahme: Bei ungültiger Länge und zu kurzem Zeitraum kommt die Meldung zur Länge (die Mindestdauer hängt von der Länge ab; bei Länge 0 sonst Division durch 0). Test `invalidIterationLengthIsReportedBeforeTooShortPeriod`.
- Annahme: `"iterationLengthWeeks": null` im JSON gilt wie ein fehlendes Feld (Standard 2); der Record bildet beides auf `null` ab.
- 3 Wochen über 04.01.–26.03.2027 (82 Tage): ⌈82/21⌉ = 4 Iterationen, IP ab 04.01. + 63 Tage = 08.03.
- Eine ID, die keine UUID ist, beantwortete Quarkus mit 404 ohne Meldung (spec-reviewer). Die Resource nimmt die ID jetzt als Text und wirft für eine ungültige dieselbe `ProgramIncrementNotFoundException`; die Meldung ist Konstante der Exception. Szenario «PI mit ungültiger ID wird nicht gefunden» im Delta unter «Neu».
- Integration im Devcontainer (docker-outside-of-docker): Testcontainers erreicht den Postgres-Port nicht über `localhost`; mit `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` grün. Umgebungsthema, nicht im Repository geändert.
