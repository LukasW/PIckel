---
name: flow-grill-me
description: Fragt ein Issue Frage für Frage durch, bis alle Entscheidungen geklärt sind, und schreibt sie nach entscheide.md im Change-Ordner. Einsetzen, wenn ein Issue unscharf ist oder vor dem Plan offene Fragen hat.
compatibility: Claude Code oder Copilot CLI (in einem vertrauten Ordner, ADR 0007) im Repository PIckel. Braucht git, gh (GitHub) und den Maven Wrapper; Projektangaben in .claude/flow.md.
argument-hint: <issue-nummer>
disable-model-invocation: true
model: opus
---

Du verhörst Issue #$ARGUMENTS, bevor auch nur eine Zeile Code geschrieben wird.

Für die Dauer dieser Session bist du **nicht** Umsetzer, sondern Interviewer. Du fragst den Nutzer so lange aus,
bis ein **geteiltes Verständnis** über jeden Aspekt des Vorhabens erreicht ist. Am Ende steht ein aufgelöster
Entscheidungsbaum, der als Eingabe für `/flow-plan` oder `/flow-implement` dient.

Ablauf-Verzeichnis: `.claude/flow` (Fragmente unter `shared/`, Skripte unter `scripts/`), relativ zum Repository-Root.

## 1. Projekt und Provider

Lies und befolge `.claude/flow/shared/project.md` und `.claude/flow/shared/git-provider.md`.

## 2. Issue und Change-Ordner lesen

Issue (Titel, Beschreibung, Labels, Kommentare) als Auftrag lesen, dann den Change-Ordner
`spec/changes/<nr>-*/` (Delta-Entwurf, frühere `entscheide.md`, Plan). Massgebend ist der Change-Ordner
(`shared/project.md` → «Das Repository führt»); was nur in einem Issue-Kommentar steht, ist eine offene Frage.

## 3. Codebase und Spezifikation erkunden (vor dem Fragen)

Kläre selbst, was du selbst klären kannst:

- Was sagen Architektur-Doku und ADRs (`flow.md` → «Spezifikation»)? Qualitätsziele, Randbedingungen,
  Datenmodell, Nicht-Ziele, Risiken, Phasenplan
- Was sagen die fachliche Spezifikation und die Story zum Issue?
- Welche Module, Schnittstellen, Use Cases, Adapter sind betroffen?
- Gibt es ähnliche Lösungen als Vorlage, auch in den Repos aus `flow.md`?
- Welche Tests und Migrationen existieren bereits?

**Goldene Regel:** Lässt sich eine Frage durch einen Blick in Codebase oder Spezifikation beantworten, dann schau
nach — frag nicht den Nutzer. Eine solche Frage kostet ihn einen Turn und zeigt, dass du nicht gelesen hast.
Widerspricht das Issue der Spezifikation (ADR, Nicht-Ziel), ist genau **das** eine Frage wert – nicht, ob die
Spezifikation gilt.

Halte vor der ersten Frage fest, was die Spezifikation schon entscheidet (Aspekt, Entscheid, Quelle). Diese Punkte
sind geklärt: Sie kommen nie als Frage, sondern im Abschluss unter «Geklärte Entscheidungen» mit ihrer Quelle.

## 4. Entscheidungsbaum aufstellen

Leite aus Issue, Codebase und Spezifikation die **offenen Verzweigungen** ab. Typische Äste:

- **Scope & Abgrenzung:** Was ist drin, was bewusst draussen? Welche Phase?
- **Schicht/Modul:** Wo gehört es hin? Braucht es ein neues Modul (dann ADR)?
- **Datenmodell:** Neue Tabelle oder Spalte? Migration? Was passiert mit bestehenden Daten? Bevor eine Migration
  bestehender Daten zur Wahl steht: zählen, was sie träfe (lesend, nur Zahlen, Weg laut `flow.md`). Trifft sie
  nichts, ist die Frage keine
- **Idempotenz:** Was passiert, wenn es zweimal läuft oder mittendrin abbricht?
- **Externe Systeme:** Was wird gelesen oder geschrieben? Kann etwas überschrieben oder verloren gehen? Limits?
- **Sicherheit:** Welche Rolle darf das? Neue ausgehende Verbindungen? Vertrauliches im Log?
- **Betrieb:** Konfiguration, Metriken, Rollen der Laufzeit?
- **Akzeptanzkriterien:** Messbar? Fehlt eines?
- Dazu die Äste aus `flow.md` → «Rückfragen»

Löse Abhängigkeiten zwischen Entscheidungen **eine nach der anderen** — eine frühe Antwort schliesst oft spätere
Äste oder eröffnet neue.

## 5. Verhör führen (interaktiv, eine Frage pro Turn)

1. **Eine Frage pro Turn.** Niemals bündeln, auch nicht als «Und noch kurz: …» oder als zweite Frage in der
   Empfehlung. Antwort abwarten, weil sie den Baum verändern kann: Eine zweite Frage im selben Turn ist oft schon
   hinfällig, und der Nutzer beantwortet von zweien meist nur eine.
2. **Immer mit Empfehlung.** Jede Frage hat deinen begründeten Vorschlag.
3. **Bei Optionen: Pro & Contra für *jede* Option**, auch für die nicht empfohlene.
4. **Gnadenlos, aber endlich.** Aufhören, wenn keine entscheidungsrelevante Unklarheit mehr offen ist.
5. **Keine Scheinfragen.** Nichts fragen, was im Change-Ordner, in der Spezifikation oder in der Codebase steht – auch
   nicht als Bestätigung («Darf X, obwohl ADR … es ausschliesst?»). Was ein ADR verbietet, ist entschieden.

**Vor dem Senden prüfen:** Enthält der Turn genau eine Frage an den Nutzer? Steht ihre Antwort in den Notizen aus
Schritt 3? Ist die zweite Antwort ja oder die erste nein, schreib den Turn neu.

**Bevorzugt das Auswahl-Werkzeug des Agenten** (in Claude Code `AskUserQuestion`): die empfohlene Option zuerst,
mit `(Empfohlen)` im Label; Pro/Contra in die `description` jeder Option. Sonst als Text:

```markdown
**Frage:** <konkrete Frage>

**Empfehlung:** <Option X> — <ein-Satz-Begründung>

**Optionen:**
- **<Option A>** (empfohlen)
  - Pro: <stichpunkt>
  - Contra: <stichpunkt>
- **<Option B>**
  - Pro: <stichpunkt>
  - Contra: <stichpunkt>
```

Nach jeder Antwort: kurz quittieren («Verstanden — also …»), den Baum aktualisieren und prüfen, ob Äste neu
entstanden oder hinfällig sind.

**Annahmen am Code prüfen, bevor sie ins Ergebnis kommen.** Sagt eine Annahme etwas über den Bestand («von Hand
gesetzt ist nie X», «jeder Typ hat genau ein Feld»), sieh im Code nach, ob er sie erzwingt (Validierung, Auswahl
in der Oberfläche, Datenbank). Erzwingt er sie nicht, ist sie keine Annahme, sondern eine offene Frage oder eine
Aufgabe für die Umsetzung.

## 6. Abschluss: Entscheidungsbaum im Repository verankern

Das Ergebnis gehört in den Change-Ordner `spec/changes/<nr>-<slug>/entscheide.md` — dort überlebt es den
Session-Wechsel und kommt mit dem PR in Git. Gibt es die Datei schon, nachführen statt überschreiben.

```markdown
# Entscheide — Issue #<nr>

## Geklärte Entscheidungen
- **<Aspekt>:** <Entscheidung> (Begründung: <kurz>)

## Bewusst draussen (Non-Goals)
- <was explizit nicht gemacht wird>

## Verbleibende Annahmen
- <falls etwas nicht final entschieden, aber als Default gesetzt wurde>

## Auswirkung auf die Spezifikation
- <Delta-Einträge, Abschnitt der Architektur-Doku, neue ADR — sonst «keine»>
```

Betrifft ein Entscheid Akzeptanzkriterien oder Szenarien, auch den Delta-Entwurf nachführen (`Status: entwurf`,
Entscheid unter «Entscheide»). Ins Issue kommt nur ein kurzer Hinweis mit dem Pfad, nach Bestätigung des Nutzers
(eine Aktion nach aussen) — nie der Inhalt.

Nächster Schritt: `/flow-plan <nr>` bzw. `/flow-implement <nr>`.

## Regeln

- Sprache des Projekts (laut `CLAUDE.md`), Du-Form
- Du implementierst **nichts** und schreibst **keinen** Plan oder Code — nur Fragen und am Ende `entscheide.md`
  sowie den Delta-Entwurf
- Im Interviewer-Modus bleiben, bis der Nutzer das Verhör beendet oder alle Äste aufgelöst sind
