---
name: flow-night-batch
description: Bereitet einen Nachtlauf vor, holt die Freigabe und startet ihn — bereite Teil-Issues eines Epics werden in Wellen unbeaufsichtigt umgesetzt, die PRs bleiben zum Review am Morgen offen. Mit «status» der Stand am Morgen, mit «stop» abbrechen.
argument-hint: <epic-nummer | issue-nummern…> | status | stop
disable-model-invocation: true
model: sonnet
---

Nachtlauf für: **$ARGUMENTS**

Ablauf-Verzeichnis: `.claude/flow` (Fragmente unter `shared/`, Skripte unter `scripts/`), relativ zum Repository-Root.
Runner: `.claude/flow/scripts/night-batch.sh` (Aufruf und Format: `--help`). Dieser Skill setzt selbst
kein Issue um. Er stellt den Lauf zusammen, holt die Freigabe und startet den Runner; der Runner startet je Issue
einen frischen `claude -p` mit `/flow-implement <nr> --nachtlauf`, nur mit `merge: ja` danach
`/flow-ship --nachtlauf` (`.claude/flow/shared/nachtlauf.md`).

## 1. Projekt und Provider

Lies und befolge `.claude/flow/shared/project.md` und `.claude/flow/shared/git-provider.md`.
Hauptzweig: `base-branch` aus dem Kopf von `flow.md`, sonst `main`. Angaben für den Lauf: `flow.md` → «Nachtlauf»
(Vorbedingungen, Warm-up, Pool, Merge, was nie in einen Nachtlauf gehört). Fehlt der Abschnitt: Voreinstellungen
(Pool 1, Merge nein, ohne Warm-up) und im Plan (Schritt 6) sagen, dass sie gelten.

## 2. Modus

- **`status`:** `night-batch.sh --status` im Repository. Zu jedem Issue, das nicht `auf-main`, `erledigt` oder
  `pr-offen` ist, den Grund holen: das letzte Ergebnis in `<ablage>/logs/<nr>.log` (Zeile mit `"type":"result"`)
  und den letzten Kommentar am Issue. Zusammenfassen: was auf dem Hauptzweig liegt, welche PRs offen sind, was
  abgebrochen ist und welche Frage dort wartet, was übersprungen wurde und worauf es wartet. Je offenem Punkt einen
  nächsten Schritt empfehlen (Frage am Issue beantworten, `/flow-grill-me <nr>`, Lauf wiederholen). Ende
- **`stop`:** `night-batch.sh --stop` im Repository, danach `--status` zeigen. Ende
- **Sonst:** weiter mit Schritt 3

Läuft für das Repository schon ein Nachtlauf (`--status` meldet «Läuft: ja»): melden und enden.

## 3. Issues sammeln

- **Epic-Nummer:** Epic samt Kommentaren lesen; die Teil-Issues aus Aufgabenliste und Text (`#<nr>`) des Epics,
  dazu offene Issues, die das Epic nennen (`gh issue list --search "#<epic>"`)
- **Liste von Nummern:** genau diese Issues
- Geschlossene Issues und solche, deren Commit schon auf `origin/<hauptzweig>` liegt, fallen weg

## 4. Reife je Issue

Für jedes Issue den Change-Ordner `spec/changes/<nr>-*/` auf dem Hauptzweig (Delta, `entscheide.md`, Plan) und
die Story lesen und einordnen; das Issue selbst ist nur Auftrag. Der
Massstab ist das Input-Gate von `/flow-implement` (Schritt 3), mit einem Unterschied: Nachts fragt niemand nach.

- **Bereit:** Change-Ordner auf dem Hauptzweig, eindeutige Akzeptanzkriterien im Delta (Story) bzw. Plan mit
  `Delta: keines — <Grund>` (Task), Scope abgegrenzt, keine offene Frage, die das Ergebnis prägt
- **Nicht bereit:** kein Change-Ordner auf dem Hauptzweig, offene Fragen, mehrere valide Lösungswege, fehlende
  Akzeptanzkriterien, oder (mit `deltas:` in
  `flow.md`) das Issue ändert bestehendes Verhalten einer Story und es gibt kein Delta mit `Status: bestätigt` auf
  dem Hauptzweig. Empfehlung: `/flow-grill-me <nr>` oder `/flow-plan <nr>` vor dem Lauf; den Change-Ordner per
  `chore/`-PR auf den Hauptzweig
- **Von Hand:** Das Issue braucht etwas, was `flow.md` → «Anhalten» oder «Nachtlauf» von Hand vorsieht (Cluster,
  Geräte, Secrets, Workflows, bestehende Migrationen), ein heikler Bereich aus «Rückfragen» ohne bestätigten Plan,
  oder ein Augenmass, das kein Test belegt
- Ein Epic oder Umbrella-Issue selbst läuft nie mit

Nur bereite Issues kommen in den Lauf. Nicht bereite und Issues «von Hand» stehen mit Grund im Plan und als
`von-hand:` in der Batch-Datei.

## 5. Abhängigkeiten und Wellen

- **Harte Abhängigkeiten:** genannt im Issue («hängt ab von», «nach #…»), in der Story, im Plan, oder sachlich (ein
  Issue braucht Typen, Tabellen oder Endpunkte eines anderen)
- **Konflikte:** Issues, die dieselben Dateien wahrscheinlich ändern (Migration, zentrale Konfiguration,
  `CLAUDE.md`, Architektur-Doku, gemeinsame Domänentypen), nicht in dieselbe Welle mit Pool > 1. Mit Pool 1 genügt
  die Reihenfolge
- **Wellen:** topologisch: Welle 1 hat keine offenen Vorgänger, jede weitere nur Vorgänger aus früheren Wellen.
  Bei Pool 1 darf ein Vorgänger auch weiter oben in derselben Welle stehen
- **Pool:** aus `flow.md` → «Nachtlauf», sonst 1. Mehr nur, wenn die Issues einer Welle sicher keine Datei teilen.
  Ein Konflikt beim Squash-Merge sperrt alle Nachfolger
- Ein Vorgänger ausserhalb des Laufs muss schon auf dem Hauptzweig liegen, sonst ist das Issue nicht bereit

## 6. Batch-Datei und Plan

Ablage: `${FLOW_NIGHT_DIR:-$HOME/night-batch}/<ordner>-<repo>/` (`<ordner>` ist der Elternordner des Repositories).
Datei `nachtlauf-<epic oder datum>.conf`. Gibt es sie schon (ein früherer Lauf zum selben Epic): lesen, an den
heutigen Stand anpassen (neue Issues, geklärte Abbrüche) und die Änderungen im Plan nennen; der Runner überspringt,
was schon auf dem Hauptzweig liegt. Format:

```text
# Nachtlauf <Projekt> — Epic #<nr> «<titel>», <datum>
repo: <absoluter pfad des repositories>
epic: <nr>
pool: 1
merge: nein
vorbedingung: <befehl aus flow.md, muss gelingen>
warmup: <befehl aus flow.md, darf scheitern>
hinweis: Nicht-interaktiver Nachtlauf zu Epic #<nr>. Massgebend ist spec/changes/<nr>-*/ auf dem Hauptzweig, nicht der Issue-Text.
von-hand: #<nr> <grund>

welle: Welle 1 — <worum es geht>
<nr>                 # <titel>
<nr>: <vorgänger>    # <titel>
```

Der `hinweis` sagt, wo die Entscheide stehen, und nennt projektspezifische Grenzen; die allgemeinen Regeln des
Nachtlaufs stehen in `shared/nachtlauf.md` und gehören nicht hinein.

Dann `night-batch.sh --dry-run <datei>`; Fehler beheben, bis die Prüfung durchläuft. Dem Nutzer vorlegen:

- Tabelle der Wellen: Issue, Titel, Vorgänger, Grund für die Welle
- Was nicht mitläuft, mit Grund und Empfehlung (Schritt 4)
- Pool, Merge, Vorbedingungen, Warm-up, Hinweis; Voreinstellungen als solche kennzeichnen
- Was nachts ohne Menschen geschieht: Mit `merge: nein` (Voreinstellung) bleiben die PRs zum Review am Morgen offen;
  Nacht-PRs sind Vorschläge, keine Merges. Issues im Lauf dürfen dann nicht aufeinander warten. `merge: ja` landet
  jedes grüne Issue ohne Review per Squash-Merge auf dem Hauptzweig, ausser PRs mit Label `needs-human-review`;
  nur nach ausdrücklichem Wunsch des Nutzers vorschlagen
- Rechte: Der Lauf hat nur die Rechte aus `.claude/settings.json` (`--permission-mode acceptEdits`), dazu einen
  Kostendeckel je Phase (`FLOW_MAX_BUDGET_USD`)
- Der Pfad der Batch-Datei

## 7. Freigabe

**Pflicht-Pause: ohne ausdrückliche Freigabe nichts starten.** Eine Frage mit Empfehlung (wie in
`/flow-grill-me`): starten, Datei anpassen, oder zuerst die nicht bereiten Issues klären. Ändert der Nutzer etwas,
Schritte 5 und 6 wiederholen.

## 8. Starten

`night-batch.sh --start <datei>`. Der Runner prüft die Datei noch einmal, läuft im Hintergrund weiter, auch wenn
diese Session endet, und hält auf macOS den Rechner mit `caffeinate` wach (nicht bei zugeklapptem Notebook ohne
Netzteil).

Ausgeben: PID, `tail -f <ablage>/logs/master.log` zum Mitlesen, `/flow-night-batch status` für den Morgen,
`/flow-night-batch stop` zum Abbrechen, und dass derselbe Start später dort fortsetzt, wo der Lauf stand.

## Regeln

- Dieser Skill ruft `/flow-implement` und `/flow-ship` nie selbst auf; jedes Issue bekommt einen frischen Kontext
- Kein Start ohne Freigabe, kein Issue im Lauf, das Schritt 4 nicht als bereit einstuft
- Die Batch-Datei liegt ausserhalb des Repositories; sie wird nicht committet
