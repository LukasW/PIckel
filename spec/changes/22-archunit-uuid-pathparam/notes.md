# Notizen — #22

- Fixtures unter `ch.grudligstrasse.pickel.archfixture.adapter.in.rest` (Testquellen): Die Regel filtert deshalb auf
  `..adapter.in.rest..` statt auf das volle Produktionspaket; `ArchitectureTest` sieht die Fixtures nicht
  (`DoNotIncludeTests`)
- `test-author` hatte die Prüfung in `ArchitectureTest` als `@ArchTest`-Methode angelegt, weil das Gerüst beim
  Initialisieren eines Felds die Discovery aller Regeln abbrach; nach der Umsetzung zurück auf ein Feld wie die
  übrigen Regeln
- UUID mit `@QueryParam`/`@HeaderParam` bleibt erlaubt, die Regel deckt nur Pfadparameter (Auftrag des Issues)
- Im Worktree fehlte `node_modules`: `npm ci` nötig, sonst scheitert `./mvnw test` an `frontend-tests` (Exit 127)
