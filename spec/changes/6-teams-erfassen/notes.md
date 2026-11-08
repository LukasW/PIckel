# Notizen US-6

Annahmen im Nachtlauf:

- REST-Vertrag steht nicht im Delta: `GET/POST /api/v1/teams`, `PUT/DELETE /api/v1/teams/{id}`; Fehler 400 (Regel,
  Duplikat), 404 (unbekannt) mit `{ "message": … }` wie bei PIs. Folgt dem Muster von US-1/US-5
- Meldung für zu langen Namen «Der Teamname darf höchstens 50 Zeichen lang sein.» analog zu PI («Der Name darf …»)
- Delta um zwei Szenarien ergänzt (Name zu lang, Bestätigung), damit AK 1 und AK 5 einen Beleg haben; kein bestehendes
  Verhalten betroffen
- Delta um fünf REST-Vertrags-Szenarien ergänzt (PUT 200, PUT-Duplikat 400, DELETE 204, 404 für unbekannte und
  ungültige ID), damit Antworten und Exception-Mapping belegt sind (wie US-5 «Unbekanntes PI»)
- Umbenennen prüft zuerst die Existenz, dann den Namen: unbekanntes Team mit ungültigem Namen → 404
- Fehlender Name (`null`, Feld fehlt) gilt als leer: «Der Teamname darf nicht leer sein.»
- Sortierung mit `Collator` de-CH (Stärke SECONDARY): Umlaute beim Grundbuchstaben, Gross-/Kleinschreibung egal
- Umbenennen im Frontend: Feld mit dem bisherigen Namen vorbelegt, dieselben Client-Prüfungen wie beim Erfassen
- Repository-Port heisst `findAllTeams`/`remove`, weil `findAll`/`delete` mit `PanacheRepositoryBase` kollidieren
- AK 5 «in keinem PI mehr»: Teams hängen am ART, nicht am PI; es gibt noch keinen Bezug Team–PI (kommt mit #13).
  Belegt ist, dass das Team aus der einzigen Teamliste verschwindet
- Race zwischen Prüfung und Insert fängt der Unique-Index (`lower(name)`); das ergäbe dann 500 statt 400 — selten,
  bewusst nicht behandelt
