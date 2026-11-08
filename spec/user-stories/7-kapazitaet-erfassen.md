# US-7: Kapazität pro Team und Iteration erfassen

**ID:** US-7
**Issue:** [#7](https://github.com/LukasW/PIkel/issues/7)
**Status:** Umgesetzt
**Priorität:** Hoch
**Schätzung:** 5

## Beschreibung

Als RTE möchte ich die Kapazität jedes Teams pro Iteration eines PI in Story Points erfassen, damit die Planung
gegen die verfügbare Kapazität geprüft werden kann.

## Akzeptanzkriterien

1. Auf der Detailseite eines PI lässt sich für jedes Team und jede Iteration ausser der IP-Iteration eine Kapazität in
   Story Points erfassen
2. Die Kapazität ist eine ganze Zahl ab 0; andere Werte werden mit einer verständlichen Meldung abgelehnt
3. Ohne Eintrag gilt die Kapazität 0
4. Die Kapazität eines Teams ist pro PI getrennt; ein Eintrag in einem PI ändert kein anderes PI
5. Ohne Teams zeigt die Tabelle einen Hinweis mit Link zur Teamseite

## BDD-Szenarien

Siehe [`src/test/resources/features/capacity/7-kapazitaet-erfassen.feature`](../../src/test/resources/features/capacity/7-kapazitaet-erfassen.feature).

## Änderungen

- #7: Kapazität pro Team und Iteration erfassen — `spec/changes/7-kapazitaet-erfassen/delta.md`
