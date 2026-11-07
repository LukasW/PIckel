# Notizen US-7

Annahmen im Nachtlauf:

- REST-Vertrag steht nicht im Delta: `GET /api/v1/program-increments/{id}/capacities` (volle Matrix),
  `PUT …/capacities/{teamId}/{iterationNumber}` mit `{ "points": n }` → 200 mit der Zelle; 400 bei Regelverletzung,
  404 bei unbekanntem PI oder Team. Folgt dem Muster von US-6/US-8
- Unbekannte Iterationsnummer (0 oder grösser als die Anzahl) → 400 «Die Iteration gibt es nicht.»
- Fehlender Wert (`points` `null` oder nicht gesetzt) → «Die Kapazität muss eine ganze Zahl ab 0 sein.»
- Dezimalzahl (`1.5`): Jackson kürzt standardmässig still auf `1`. Erst global abgeschaltet; der Spec-Reviewer
  wies darauf hin, dass das US-5 und US-9 ändert (wäre «Geändert», nachts nicht erlaubt). Jetzt nur für
  `CapacityRequest.points` über `WholeNumberDeserializer` → 400 «Die Anfrage enthält einen ungültigen Wert.»
  (allgemeine Meldung des bestehenden Mappers). Die Oberfläche meldet Kommazahlen vorher mit der Meldung der Domain
- Formular prüft vor dem Senden: Kommazahl, negative Zahl oder Text («abc», `validity.badInput`) → Alarm «Die
  Kapazität muss eine ganze Zahl ab 0 sein.», kein Request, die Zelle zeigt wieder den gespeicherten Wert
- Migration: Beim Rebase lag US-9 mit `V5__create_features.sql`/changeSet 5 schon auf `main` → Kapazität ist
  `V6__create_capacity.sql`/changeSet 6
- Cucumber: `LastResponse` des Test-Autors durch den `ScenarioContext` aus US-9 ersetzt (gleicher Zweck); dort
  `lastResponse()` ergänzt, damit «wird die Änderung mit dem Status …» auch Kapazitäts-Antworten prüft
- Keine Obergrenze für die Kapazität (Delta nennt keine); die DB-Spalte ist `INTEGER`
- Fremdschlüssel `ON DELETE CASCADE` auf Team und PI: Ein entferntes Team nimmt seine Kapazität mit (US-6 AK 5)
- Frontend: Zelle speichert bei `change` (Verlassen/Enter), ein geleertes Feld speichert nicht und zeigt wieder den
  gespeicherten Wert; nach einem abgelehnten Wert zeigt die Zelle wieder den gespeicherten Wert, die Meldung als Alarm
- Frontend zeigt 0 auch für eine Zelle, die der Server nicht liefert (Test aus `test-author`); zweiter Ort für
  «ohne Eintrag 0», nur in der Anzeige, bewusst behalten, weil ein leeres Feld irreführend wäre
- Texte: Leerzustand «Noch keine Teams. Erfasse zuerst die Teams.» mit Link «Zu den Teams»; Überschrift
  «Kapazität in Story Points»; Feld-Label «Kapazität <Team>, <Iteration>»
- Service ersetzt nach dem Speichern nur die Zelle statt neu zu laden, damit Eingaben in anderen Zellen bleiben
