# Notizen US-8

Annahmen im Nachtlauf (Delta schweigt, vertretbar):

- Text und Links gehören zum Aggregat PI (ADR 0005: «Am PI hängen …»).
- Link entfernen mit unbekannter oder ungültiger ID → 404 «Den Link gibt es nicht.» (eigene Exception und Mapper,
  wie beim unbekannten PI).
- Keine Obergrenze für die URL-Länge und die Zahl der Links (Delta nennt keine); Spalte `url` ist `TEXT`.
- Texte werden nicht getrimmt (Zeilenumbrüche bleiben); `null` bzw. ein fehlendes Feld gilt als leerer Text. Das
  Formular misst deshalb mit `Validators.maxLength`, nicht mit `maxTrimmedLength` (Befund angular-signals-reviewer
  verworfen).
- Das URL-Schema wird ohne Rücksicht auf Gross- und Kleinschreibung geprüft (RFC 3986): `HTTPS://…` gilt,
  `JAVASCRIPT:` nicht. Gespeichert wird die URL wie eingegeben.
- Der Titel wird wie der PI-Name ohne Leerzeichen am Rand gespeichert und danach gemessen.
- Der Hinweis «Noch kein Business-Kontext erfasst.» erscheint nur, wenn beide Texte leer sind und kein Link da ist.
- Alle drei Änderungs-Endpunkte antworten 200 mit dem ganzen PI (auch `POST …/links`), damit die Oberfläche ohne
  zweiten Abruf nachzieht.
- Bedienung der Detailseite: Formulare für Texte und Links stehen immer unter der Anzeige, je Link ein Knopf
  «Entfernen»; Meldungen des Servers erscheinen über dem Bereich, das PI bleibt sichtbar.

Unterwegs:

- Adapter-`save` ist jetzt ein Upsert. Bestehende Link-Entities werden über die ID wiederverwendet: Hibernate fügt
  beim Flush vor dem Löschen ein, ein neues Objekt mit derselben ID verletzte sonst den Primärschlüssel.
- Kein `UNIQUE (program_increment_id, position)` in V3: Beim Entfernen in der Mitte schreibt Hibernate die Positionen
  zeilenweise neu und verletzte die Bedingung zwischendurch.
- Links hinzufügen/entfernen setzt das Kontext-Formular nicht zurück, damit ungespeicherter Text bleibt (Befund
  angular-signals-reviewer, Test «behält ungespeicherten Business-Kontext …»).
- Szenarien «Business-Kontext wird geändert» und «Link wird entfernt» im Delta unter «Neu» ergänzt: Überschreiben,
  Entfernen und die Reihenfolge der Links liessen sich nur gegen PostgreSQL belegen (Befund test-reviewer,
  spec-reviewer).
