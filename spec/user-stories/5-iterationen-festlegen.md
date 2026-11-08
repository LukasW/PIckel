# US-5: Iterationen eines PI mit IP-Iteration festlegen

**ID:** US-5
**Issue:** [#5](https://github.com/LukasW/PIckel/issues/5)
**Status:** Umgesetzt
**Priorität:** Hoch
**Schätzung:** 5

## Beschreibung

Als RTE möchte ich beim Anlegen eines PI die Iterationslänge wählen und die Iterationen samt IP-Iteration sehen,
damit das Program Board die Spalten des PI kennt.

## Akzeptanzkriterien

1. Beim Anlegen eines PI ist die Iterationslänge in Wochen wählbar, Standard 2, erlaubt 1 bis 4
2. Die Iterationen folgen lückenlos ab dem Startdatum; jede endet am Tag vor dem Beginn der nächsten, die letzte am
   Enddatum des PI
3. Die letzte Iteration ist die IP-Iteration und heisst «IP», die übrigen heissen «Iteration 1», «Iteration 2» usw.
4. Ein Zeitraum, der weniger als zwei Iterationen ergibt, wird mit einer verständlichen Meldung abgelehnt
5. Die Detailseite eines PI zeigt seine Iterationen mit Name, Start- und Enddatum (TT.MM.JJJJ); die IP-Iteration ist
   sichtbar markiert
6. Ein unbekanntes PI wird mit einer verständlichen Meldung abgelehnt

## BDD-Szenarien

Siehe [`src/test/resources/features/program-increments/5-iterationen-festlegen.feature`](../../src/test/resources/features/program-increments/5-iterationen-festlegen.feature).

## Änderungen

- #5: Iterationen eines PI mit IP-Iteration festlegen — `spec/changes/5-iterationen-festlegen/delta.md`
