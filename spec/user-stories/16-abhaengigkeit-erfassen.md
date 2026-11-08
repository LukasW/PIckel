# US-16: Abhängigkeit zwischen Features erfassen

**ID:** US-16
**Issue:** [#16](https://github.com/LukasW/PIkel/issues/16)
**Status:** Umgesetzt
**Priorität:** Hoch
**Schätzung:** 5

## Beschreibung

Als Teilnehmerin oder Teilnehmer des PI Planning möchte ich festhalten, dass ein Feature ein anderes Feature desselben PI braucht, damit
Abhängigkeiten zwischen Teams im Planning erfasst sind und nicht im Kopf einzelner Personen.

Feature A **braucht** Feature B; B ist die Lieferung, A der Bedarf. Der Zeitpunkt ergibt sich aus der Einplanung
beider Features, es gibt kein eigenes Datum. Die Linie auf dem Board folgt mit #17.

## Akzeptanzkriterien

1. Für ein Feature lässt sich festhalten, dass es ein anderes Feature desselben PI braucht
2. Ein Feature kann sich nicht selbst brauchen; doppelte und direkt umgekehrte Abhängigkeiten werden abgelehnt
   (längere Zyklen erkennt PIkel in M1 nicht)
3. Beim Feature sind die Abhängigkeiten in beide Richtungen sichtbar («braucht» und «wird gebraucht von»)
4. Eine Abhängigkeit lässt sich entfernen
5. Wird ein Feature gelöscht, verschwinden auch seine Abhängigkeiten

## BDD-Szenarien

Siehe [`src/test/resources/features/features/16-abhaengigkeit-erfassen.feature`](../../src/test/resources/features/features/16-abhaengigkeit-erfassen.feature).

## Tests

| Kriterium | Test |
|---|---|
| 1 Abhängigkeit im selben PI festhalten | `FeatureDependencyTest` → `featureCanNeedAnotherFeatureOfSameProgramIncrement`, `featureCanNeedSeveralFeatures`, `featureOfOtherProgramIncrementIsRejected`; `FeatureDependencyServiceTest` → `addSavesAndReturnsDependencyInRequestedDirection`, `addRejectsNeededFeatureOfOtherProgramIncrement`; CucumberIT «Abhängigkeit wird erfasst», «Abhängigkeit zu einem Feature aus einem anderen PI wird abgelehnt»; Vitest `feature-backlog.component.spec.ts` → «hält fest, dass ein Feature das gewählte Feature braucht, und zeigt es danach an (US-16)», «bietet je Feature die anderen Features des Backlogs zur Auswahl an (US-16)» |
| 2 Selbstbezug, doppelt, direkte Umkehr abgelehnt | `FeatureDependencyTest` → `featureCannotNeedItself`, `duplicateDependencyIsRejected`, `directReverseDependencyIsRejected`, `longerCycleIsNotRejected`; `FeatureDependencyServiceTest` → `addRejectsFeatureNeedingItself`, `addRejectsDuplicateOfStoredDependency`, `addRejectsReverseOfStoredDependency`; CucumberIT «Feature, das sich selbst braucht, wird abgelehnt», «Doppelte Abhängigkeit wird abgelehnt», «Umgekehrte Abhängigkeit wird abgelehnt»; Vitest → «zeigt die Meldung des Servers als Alarm, wenn er die Abhängigkeit ablehnt (US-16)» |
| 3 Beide Richtungen sichtbar | CucumberIT «Abhängigkeit wird erfasst» (beide Richtungen über die Liste); Vitest `feature-dependencies.service.spec.ts` → «nennt die Features, die ein Feature braucht, …», «nennt die Features, die ein Feature brauchen, …»; `feature-backlog.component.spec.ts` → «zeigt beim Feature, welches Feature es braucht (US-16)», «zeigt beim gebrauchten Feature, von welchem Feature es gebraucht wird (US-16)», «zeigt bei einem Feature beide Richtungen getrennt (US-16)» |
| 4 Abhängigkeit entfernen | `FeatureDependencyServiceTest` → `removeDeletesDependency`, `removeRejectsNeedingFeatureOfOtherProgramIncrementAsNotFound`; CucumberIT «Abhängigkeit wird entfernt»; Vitest → «entfernt eine Abhängigkeit und zeigt sie danach nicht mehr (US-16)», «entfernt nur die gewählte Abhängigkeit (US-16)» |
| 5 Gelöschtes Feature nimmt Abhängigkeiten mit | CucumberIT «Gelöschtes Feature nimmt seine Abhängigkeiten mit», «Gelöschtes brauchendes Feature nimmt seine Abhängigkeiten mit»; Vitest → «lädt nach dem Löschen eines Features dessen Abhängigkeiten neu, sie verschwinden (US-16)» |

## Änderungen

- #16: Abhängigkeit zwischen Features erfassen — `spec/changes/16-abhaengigkeit-erfassen/delta.md`
