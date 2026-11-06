---
paths:
  - "src/main/java/**"
  - "src/test/java/**"
  - "src/main/resources/db/**"
---

# Java-Konventionen

- **Records für Domain-Modelle und DTOs**, Validierung im Compact Constructor — ein ungültiges Objekt kann dann gar
  nicht entstehen
- **Constructor Injection**, `@ApplicationScoped` für Services und Adapter — Abhängigkeiten sind sichtbar und im
  Unit-Test ohne CDI ersetzbar
- **`@Transactional` im Application-Service**, nicht in Resource oder Adapter — die Transaktion umfasst genau einen
  Use Case
- **DB-Spalten snake_case.** Schema nur über eine neue Migration `db/migration/V<n>__<slug>.sql` plus Eintrag in
  `db/changeLog.xml`; bestehende Migrationen nie ändern, weil Liquibase Checksummen prüft
- **Formatierung macht Spotless** (Palantir Java Format): `./mvnw spotless:apply` statt von Hand einrücken;
  `verify` und CI scheitern an unformatiertem Code
- **Namen:** Domain `ProgramIncrement`, Entity `ProgramIncrementEntity`, Port `ProgramIncrementRepository`, Adapter
  `ProgramIncrementRepositoryAdapter`, Service `ProgramIncrementService` implementiert `ProgramIncrementUseCase`,
  REST `ProgramIncrementResource`
- **Testmethoden ohne Story-Suffix**; den Bezug zur Story trägt das Feature als `# JUnit: <Pfad>/<Klasse> → <methode>`
