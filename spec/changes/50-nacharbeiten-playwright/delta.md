# Delta US-13 — Feature auf dem Board einplanen (Issue #50)

Story: `spec/user-stories/13-feature-einplanen.md` (bestehend) · Status: eingepflegt

## Warum

Im Playwright-Test landete eine Karte, die über der IP-Iteration losgelassen wurde, kommentarlos in Iteration 4. AK 3
verlangt eine Ablehnung mit Meldung. Die Ursache betrifft jedes Loslassen ausserhalb einer Ablage: Das Board legt die
Karte dann in der zuletzt überfahrenen Zelle ab.

## Entscheide

- **Ausserhalb einer Ablage losgelassen:** Die Karte bleibt an ihrem bisherigen Ort, ohne Meldung. Das ist das übliche
  Verhalten beim Ziehen und bisher nicht beschrieben (Plan)
- **Über der IP-Iteration losgelassen:** Die Karte bleibt am bisherigen Ort, und über dem Board erscheint die Meldung
  aus AK 3. Die IP-Zelle bleibt keine Ablage; es gibt also keine Vorschau, die ein Einplanen vortäuscht (Plan)

## Neu

### AK: Wird eine Karte ausserhalb einer Ablage losgelassen, bleibt sie an ihrem bisherigen Ort; über der IP-Iteration erscheint dazu die Meldung aus AK 3

### Szenario: Karte über der IP-Iteration losgelassen bleibt an ihrem Ort
- **Gegeben sei** das Feature "Portal" bei "Adler" in "Iteration 2" eingeplant
- **Wenn** ich "Portal" über die Zellen von "Adler" ziehe und über "IP" loslasse
- **Dann** liegt "Portal" weiter bei "Adler" in "Iteration 2"
- **Und** über dem Board steht "In der IP-Iteration wird keine Feature-Arbeit geplant."

### Szenario: Karte ausserhalb einer Ablage losgelassen bleibt an ihrem Ort
- **Gegeben sei** das Feature "Portal" im Backlog des Boards
- **Wenn** ich "Portal" über die Zelle von "Adler" in "Iteration 1" ziehe und neben dem Raster loslasse
- **Dann** liegt "Portal" weiter im Backlog des Boards
- **Und** über dem Board steht keine Meldung

## Geändert

keine

## Entfernt

keine

---

# Delta US-1 — PI anlegen (Issue #50)

Story: `spec/user-stories/1-pi-anlegen.md` (bestehend) · Status: eingepflegt

## Warum

Die PI-Übersicht ist heute nur über das Logo erreichbar; die Kopfzeile verlinkt nur «Teams».

## Entscheide

keine

## Neu

### AK: Die Kopfzeile verlinkt auf jeder Seite die PI-Übersicht («Program Increments») und die Teams

### Szenario: Kopfzeile führt zur PI-Übersicht
- **Gegeben sei** eine beliebige Seite von PIckel
- **Wenn** ich in der Kopfzeile "Program Increments" wähle
- **Dann** sehe ich die Liste der PIs

## Geändert

keine

## Entfernt

keine

---

# Delta US-12 — Program Board eines PI anzeigen (Issue #50)

Story: `spec/user-stories/12-program-board-anzeigen.md` (bestehend) · Status: eingepflegt

## Warum

Die Team-Spalte hat keine sichtbare Überschrift, Kopfzeile und erste Team-Zeile gehen ohne Linie ineinander über, und
ein leeres Backlog ist nur eine leere Box.

## Entscheide

- **Leeres Backlog:** zwei Hinweise. Sind alle Features eingeplant: «Alle Features sind eingeplant.» Hat das PI noch
  keine Features: «Noch keine Features.» mit Link auf die Detailseite des PI, wo sie erfasst werden (Plan)

## Neu

### AK: Die Spalte der Teams trägt sichtbar die Überschrift «Team»; eine Linie trennt die Kopfzeile von der ersten Team-Zeile (Linie manuell belegt: Screenshot im PR)

### AK: Ein leeres Backlog zeigt einen Hinweis: ohne Features im PI einen Link zu dessen Detailseite, sonst «Alle Features sind eingeplant.»

### Szenario: Board zeigt die Überschrift der Team-Spalte
- **Gegeben sei** das Board eines PI mit den Teams "Adler" und "Zebra"
- **Wenn** ich das Board öffne
- **Dann** trägt die erste Spalte die Überschrift "Team"

### Szenario: Leeres Backlog auf dem Board zeigt einen Hinweis
- **Gegeben sei** das Board eines PI, dessen Features alle eingeplant sind
- **Wenn** ich das Board öffne
- **Dann** zeigt das Backlog "Alle Features sind eingeplant."

### Szenario: Board ohne Features verweist auf die Detailseite
- **Gegeben sei** das Board eines PI ohne Features
- **Wenn** ich das Board öffne
- **Dann** zeigt das Backlog "Noch keine Features." mit einem Link zur Detailseite des PI

## Geändert

keine

## Entfernt

keine

---

# Delta US-11 — Features per CSV importieren (Issue #50)

Story: `spec/user-stories/11-csv-import.md` (bestehend) · Status: eingepflegt

## Warum

Die Oberfläche nennt das erwartete Format nicht; wer zum ersten Mal importiert, muss die Spalten raten.

## Entscheide

- **Beispieldatei:** Die Seite erzeugt sie selbst (Link mit `download`, Inhalt aus einer Konstante); keine
  statische Datei und keine Änderung an der Build-Konfiguration (Plan)

## Neu

### AK: Beim Import nennt die Oberfläche die Spalten `Titel` (Pflicht), `Beschreibung`, `WSJF-Rang`, `Owner` und `Schätzung` und bietet eine Beispieldatei zum Herunterladen

### Szenario: Oberfläche nennt das CSV-Format und bietet eine Beispieldatei
- **Gegeben sei** das Backlog eines PI
- **Wenn** ich den Import ansehe
- **Dann** nennt die Oberfläche die Spalten "Titel", "Beschreibung", "WSJF-Rang", "Owner" und "Schätzung"
- **Und** eine Beispieldatei mit dieser Kopfzeile lässt sich herunterladen

## Geändert

keine

## Entfernt

keine

---

# Delta US-16 — Abhängigkeit zwischen Features erfassen (Issue #50)

Story: `spec/user-stories/16-abhaengigkeit-erfassen.md` (bestehend) · Status: eingepflegt

## Warum

Im Test stand die Meldung einer abgelehnten Abhängigkeit über der ganzen Liste, weit weg von der Zeile, und blieb
nach weiteren Aktionen stehen. Die Auswahl bot schon verbundene Features an, der Knopf ohne Wahl tat nichts.

## Entscheide

- **Auswahl:** ohne Features, mit denen das Feature schon in einer der beiden Richtungen verbunden ist. Die
  Server-Regeln aus AK 2 bleiben unverändert und weiter geprüft (Plan)
- **Meldung:** bei der Zeile, deren Aktion scheiterte; sie verschwindet mit der nächsten Aktion an Abhängigkeiten
  (Plan)

## Neu

### Szenario: Auswahl bietet nur noch nicht verbundene Features an
- **Gegeben sei** das Feature "Shop", das "Login" braucht, und das Feature "Suche"
- **Wenn** ich bei "Shop" die Auswahl "Braucht Feature" öffne
- **Dann** steht dort "Suche", aber weder "Login" noch "Shop"
- **Und** ohne Wahl zeigt sie "Feature wählen …" und "Abhängigkeit hinzufügen" ist nicht aktiv

### Szenario: Abgelehnte Abhängigkeit meldet sich beim Feature
- **Gegeben sei** das Backlog mit den Features "Shop" und "Suche"
- **Wenn** der Server die Abhängigkeit von "Shop" zu "Suche" ablehnt
- **Dann** steht seine Meldung in der Zeile von "Shop" und nicht über der Liste
- **Und** sie verschwindet, sobald ich die nächste Abhängigkeit erfasse oder entferne

## Geändert

### AK: Die Auswahl bietet die anderen Features des Backlogs an (bisher ohne eigenes Kriterium, belegt unter AK 1)
- **Vorher:** Jedes andere Feature des Backlogs steht zur Wahl, die erste Option ist leer; «Abhängigkeit hinzufügen»
  ohne Wahl tut nichts
- **Nachher:** Zur Wahl stehen nur Features, mit denen das Feature in keiner Richtung verbunden ist; ohne Wahl zeigt
  die Auswahl «Feature wählen …», und «Abhängigkeit hinzufügen» ist nicht aktiv
- **Grund:** Doppelte und umgekehrte Abhängigkeiten liessen sich wählen, nur um dann abgelehnt zu werden
- **Tests:** Vitest `feature-backlog.component.spec.ts` → «bietet je Feature die anderen Features des Backlogs zur
  Auswahl an (US-16)», «hält ohne Auswahl keine Abhängigkeit fest (US-16)»

### AK: Meldungen zu Abhängigkeiten (bisher belegt unter AK 2 und 4)
- **Vorher:** Lehnt der Server ab oder scheitert das Entfernen, steht die Meldung als Alarm über der Liste und bleibt
  bis zur nächsten erfolgreichen Aktion an Abhängigkeiten
- **Nachher:** Die Meldung steht als Alarm in der Zeile des Features, dessen Aktion scheiterte, und verschwindet mit
  der nächsten Aktion an Abhängigkeiten
- **Grund:** Bei langem Backlog war nicht zu sehen, zu welcher Zeile die Meldung gehört
- **Tests:** Vitest `feature-backlog.component.spec.ts` → «zeigt die Meldung des Servers als Alarm, wenn er die
  Abhängigkeit ablehnt (US-16)», «zeigt die Meldung des Servers als Alarm, wenn das Entfernen scheitert (US-16)»

## Entfernt

keine

---

# Delta US-10 — Backlog nach WSJF-Rang bearbeiten (Issue #50)

Story: `spec/user-stories/10-backlog-bearbeiten.md` (bestehend) · Status: eingepflegt

## Warum

«Bearbeiten» lud das Feature in das Erfassungsformular über der Liste; bei langem Backlog war die bearbeitete Zeile
danach nicht mehr zu sehen.

## Entscheide

- **Ort:** Bearbeitet wird in der Zeile des Features; das Formular oben dient nur dem Erfassen (Rückfrage im Plan,
  2026-11-08)
- **Eins zugleich:** Öffnet man ein zweites Feature zum Bearbeiten, schliesst das erste ohne zu speichern (Plan)

## Neu

### Szenario: Feature wird in seiner Zeile bearbeitet
- **Gegeben sei** das Backlog mit den Features "Login" und "Shop"
- **Wenn** ich bei "Shop" "Bearbeiten" wähle
- **Dann** wird die Zeile von "Shop" zum Formular mit seinen Angaben
- **Und** das Formular zum Erfassen bleibt leer
- **Und** nach "Speichern" zeigt die Zeile die neuen Angaben

## Geändert

### AK: Ein Feature lässt sich bearbeiten, mit denselben Regeln wie beim Erfassen
- **Vorher:** Ein Feature lässt sich bearbeiten, mit denselben Regeln wie beim Erfassen (im Formular über der Liste)
- **Nachher:** Ein Feature lässt sich in seiner Zeile bearbeiten, mit denselben Regeln wie beim Erfassen; das
  Formular zum Erfassen bleibt dabei leer, und es wird höchstens ein Feature zugleich bearbeitet
- **Grund:** Kontextverlust bei langem Backlog
- **Tests:** Vitest `feature-backlog.component.spec.ts` → alle Tests mit «Bearbeiten» aus US-10 (u. a. «füllt beim
  Bearbeiten das Formular in der Zeile mit den Angaben des Features», «verwirft beim Abbrechen des Bearbeitens die Änderungen und
  erfasst danach wieder neu», «zeigt die Meldung des Servers als Alarm und behält die Angaben, wenn das Bearbeiten
  abgelehnt wird»): sie suchen das Formular in der Zeile statt oben. CucumberIT «Feature wird bearbeitet» und
  «Bearbeiten mit leerem Titel wird abgelehnt» bleiben (REST-Vertrag)

## Entfernt

keine

---

# Delta US-8 — Business-Kontext und ART-Ziele am PI (Issue #50)

Story: `spec/user-stories/8-business-kontext.md` (bestehend) · Status: eingepflegt

## Warum

Business-Kontext und PI Objectives standen zweimal auf der Seite: als Anzeige und direkt darunter im stets offenen
Formular.

## Entscheide

- **Anzeige mit Knopf:** Die Anzeige bleibt; das Formular öffnet erst über «Bearbeiten», das Link-Formular über
  «Link hinzufügen». Ohne Inhalt steht beim Hinweis der Knopf «Erfassen» (Rückfrage im Plan, 2026-11-08)

## Neu

### Szenario: Business-Kontext wird über Bearbeiten geändert
- **Gegeben sei** ein PI mit dem Business-Kontext "Digitalisierung"
- **Wenn** ich die Detailseite öffne
- **Dann** steht "Digitalisierung" einmal auf der Seite und kein Eingabefeld dafür
- **Wenn** ich "Bearbeiten" wähle, den Text ändere und speichere
- **Dann** zeigt die Seite den neuen Text und das Formular ist wieder zu

## Geändert

### AK: Die Detailseite des PI zeigt Text mit Zeilenumbrüchen und die Links, die sich in einem neuen Tab öffnen
- **Vorher:** Die Detailseite zeigt Text und Links; darunter stehen immer offen die Formulare für Business-Kontext,
  PI Objectives und Links
- **Nachher:** Die Detailseite zeigt Text mit Zeilenumbrüchen und die Links, die sich in einem neuen Tab öffnen. Das
  Formular für Business-Kontext und PI Objectives öffnet sich über «Bearbeiten» (ohne Inhalt «Erfassen»), das für
  Links über «Link hinzufügen»; Speichern oder Abbrechen schliesst es, Abbrechen verwirft die Eingabe
- **Grund:** Doppelte Darstellung, lange Seite
- **Tests:** Vitest `program-increment-detail.component.spec.ts` → die Tests zum Erfassen aus US-8 (u. a. «füllt die
  Eingabefelder mit den erfassten Texten», «erfasst Business-Kontext und PI Objectives und zeigt das gespeicherte
  PI», «fügt einen Link hinzu, zeigt ihn und leert das Formular», «behält ungespeicherten Business-Kontext, wenn ein
  Link hinzukommt»): sie öffnen das Formular zuerst. CucumberIT-Szenarien bleiben (REST-Vertrag)

## Entfernt

keine
