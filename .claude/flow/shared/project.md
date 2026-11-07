# Fragment: Projektangaben laden

Jeder Skill von `flow` beginnt hier.

1. `.claude/flow.md` im Wurzelordner des Repositories lesen (`git rev-parse --show-toplevel`). Sie sagt, wo
   Spezifikation, Stories und Pläne liegen, wie gebaut wird, welche Reviewer es gibt und was das Projekt
   zusätzlich verlangt. Wo ein Skill «laut `flow.md`» schreibt, gilt der genannte Abschnitt.
2. Fehlt die Datei: abbrechen und melden. Ohne Bestätigung nichts anlegen.
3. Fehlt nur ein Abschnitt: die Voreinstellung des Skills nehmen. Hängt davon eine Aktion nach aussen ab (Labels,
   Merge-Strategie), nachfragen.
4. `CLAUDE.md` und `.claude/rules/` gelten weiter; bei Widerspruch zu `flow.md` gewinnt `CLAUDE.md`, und der
   Widerspruch wird gemeldet.

Der Git-Provider: `.claude/flow/shared/git-provider.md`.

Wann du anhältst und fragst, statt weiterzumachen: `.claude/flow/shared/stop-conditions.md`. Gilt für
jeden Skill und jeden Agenten von flow, auch wenn der Skill es nicht noch einmal nennt.

## Das Repository führt, das Issue verweist

Massgebend sind die Story (`flow.md` → «User Stories») und der Change-Ordner `spec/changes/<nr>-<slug>/` im
Repository, nicht das Issue. Ein Issue lässt sich ohne Review ändern, verschwindet nicht in Git und läuft der Story
deshalb davon; eine Spezifikation an zwei Orten widerspricht sich früher oder später.

- Das Issue ist Auftrag und Verweis: Titel, ein Absatz Zielbild, Link auf den Change-Ordner. Akzeptanzkriterien,
  Szenarien, Entscheide und Plan stehen nur im Change-Ordner (`.claude/flow/shared/story-delta.md` → «Ort»)
- Widersprechen sich Issue und Repository: Es gilt das Repository. Das Issue wird nicht nachgeführt; eine neue
  Information aus dem Issue (etwa ein Kommentar) kommt erst nach Rückfrage ins Delta
- Was nur im Issue steht, gilt als nicht spezifiziert
