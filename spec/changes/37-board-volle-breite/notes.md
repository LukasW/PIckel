# Notizen — Issue #37

- `app.spec.ts` rendert `App` mit `TestBed.createComponent` und `Router.navigateByUrl` statt `RouterTestingHarness`
  (Plan): Der Harness rendert eine eigene Wurzel mit `router-outlet`, `nav` und `main` aus `App` fehlten im DOM
- Der Test «begrenzt auf der PI-Liste …» war schon vor der Umsetzung grün; er schützt davor, dass alle Seiten breit
  werden, und ist darum kein Rot-Beleg
- 6a (Gerüst) entfiel: keine neue öffentliche Signatur, `fullWidth` ist ein geschütztes Signal in `App`
