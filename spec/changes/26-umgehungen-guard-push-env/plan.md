# Task — Umgehungen von guard-push und guard-env schliessen (Issue #26)

Delta: keines — Harness-Fix unter `.claude/flow/scripts/`, kein fachliches Verhalten einer Story von PIckel.

## Übersicht

Das Sicherheits-Review zu #24 hat Umgehungen gefunden, die auf `main` bestehen (nachgestellt am 2026-11-07 auf
`55e0a4b`, je Exit 0 = durchgelassen):

| Guard | Eingabe | heute |
|---|---|---|
| guard-push | `git -c k=v push origin main` | 0 |
| guard-push | `git --no-pager push origin main` | 0 |
| guard-push | `"git" push origin main` | 0 |
| guard-push | `git push origin \` + Umbruch + `HEAD:main` | 0 |
| guard-push | `git push origin 'refs/heads/*:refs/heads/*'` (Glob-Refspec, nicht im Issue) | 0 |
| guard-push | `git push --all origin` auf einem Feature-Zweig (nicht im Issue) | 0 |
| guard-env | `cat .en*`, `cat .en[v]`, `cat {.env,x}` (Platzhalter) | 0 |
| guard-env | `cat .e""nv`, `cat '.e'nv`, `cat .\env` (getrennte Anführungszeichen, Backslash) | 0 |
| guard-env | Symlink auf die Geheimnis-Datei, gelesen über Bash oder Read | 0 |

Ursache in beiden Guards: Sie prüfen den rohen Befehlstext mit einem Muster, das nur die wörtliche Schreibweise
kennt. Die Shell entfernt aber Anführungszeichen und Backslashes, fügt Zeilenfortsetzungen zusammen und expandiert
Platzhalter, bevor `git` oder `cat` laufen.

**Ansatz:** Den Befehl vor der Prüfung so normalisieren, wie die Shell ihn sieht (eine gemeinsame Funktion in
`lib.sh`), und dann auf den Wörtern prüfen statt auf dem Rohtext. Dazu bei `guard-push` die Erkennung von
«git … push» lockern (beliebige globale Optionen dazwischen), bei `guard-env` Platzhalter-Wörter wie in der Shell
gegen die Geheimnis-Namen abgleichen und Symlinks auflösen.

**Bedrohungsmodell (neu ausdrücklich):** Die Guards fangen Versehen und naheliegende Umgehungen ab, wie sie ein
Agent oder ein eingeschleuster Issue-Text erzeugt. Sie sind keine Sandbox: Wer Code ausführen kann, kommt über ein
Skript, eine Variable (`$G push`), `eval` mit zusammengesetztem Text, einen Git-Alias oder `grep -r` über einen
ganzen Ordner weiter an Push oder Geheimnis. Diese Restrisiken stehen danach im Kopf der Skripte und in ADR 0007,
damit niemand mehr Schutz annimmt, als es gibt.

## Zu erstellende Dateien

- [x] `spec/changes/26-umgehungen-guard-push-env/notes.md` — Überraschungen und Annahmen während der Umsetzung

## Zu ändernde Dateien

- [x] `.claude/flow/scripts/lib.sh` — neue Funktion `flow_shell_segments`: normalisierter Befehl, ein Segment pro
  Zeile
- [x] `.claude/flow/scripts/test-hook-input.sh` — Fälle für `flow_shell_segments`
- [x] `.claude/flow/scripts/guard-push.sh` — Erkennung und Prüfung auf normalisierten Segmenten, `--all`/`--branches`,
  Glob-Refspec, Push ohne Refspec über `@{push}`; Kopfkommentar mit Restrisiken
- [x] `.claude/flow/scripts/test-guard-push.sh` — die Umgehungen als Fälle, dazu erlaubte Fälle (Push mit `-c
  credential…`, Commit ohne Push)
- [x] `.claude/flow/scripts/guard-env.sh` — Bash-Zweig auf normalisiertem Text, Platzhalter-Wörter, Symlinks
  (Bash und Lese-Werkzeuge); Musterabgleich als eine Funktion für beide Zweige; Kopfkommentar mit Restrisiken
- [x] `.claude/flow/scripts/test-guard-env.sh` — die Umgehungen als Fälle, Symlink-Fälle in einem temporären
  Ordner, erlaubte Fälle gegen Fehlalarme
- [x] `docs/adr/0007-copilot-als-zweiter-agent.md` — unter «Konsequenzen» ein Punkt zum Bedrohungsmodell (siehe
  Entscheid 3)

## Architektur-Entscheide

- **Schicht:** nur Harness (`.claude/flow/scripts/`), kein Java, kein Angular; `ArchitectureTest` unberührt.
  Abhängigkeiten bleiben `bash` und `jq`, kein Python oder `shlex`
- **Normalisierung in `lib.sh`, einmal für beide Guards** (`flow_shell_segments <befehl>`):
  1. Zeilenfortsetzung (`\` + Umbruch) entfernen
  2. In Segmente trennen an `;`, `&`, `|`, Umbruch, `(`, `)`, `` ` `` — damit auch `$(…)`, Subshells und
     `sh -c "…"` (nach Schritt 3) als eigene Befehle geprüft werden
  3. `"`, `'` und `\` entfernen: `"git"` → `git`, `.e""nv` → `.env`, `.\env` → `.env`

  Bewusst keine echte Wortzerlegung mit Anführungszeichen: Ein Text in Anführungszeichen wird wie ein Befehl
  geprüft. Das ist fail closed: `sh -c "git push origin main"` bleibt erkannt, dafür wird auch
  `git commit -m "push origin main"` abgelehnt. Ausweg wie schon bei `guard-env`: `git commit -F <datei>`
- **guard-push erkennt einen Push breiter:** Ein Segment ist ein Push, wenn ein Wort mit Basisname `git` (auch
  `/usr/bin/git`) vorkommt und danach ein Wort `push`. Was dazwischen steht (`-c k=v`, `--no-pager`, `-C dir`,
  `--git-dir=…`), spielt keine Rolle. Geprüft werden die Wörter nach `push` wie heute. Der Push aus dem Devcontainer
  mit `-c credential…helper=!gh auth git-credential` (Gedächtnis «Push im Devcontainer») bleibt erlaubt; dafür gibt
  es einen eigenen Fall
- **guard-push, Zusätze über das Issue hinaus** (Entscheid 1): `--all` und `--branches` ablehnen; ein Refspec
  mit Platzhalter wird abgelehnt, wenn sein Ziel als Muster auf `main` passt (`refs/heads/*`); ein Push ohne Refspec
  wird zusätzlich über `git rev-parse --symbolic-full-name @{push}` geprüft (Upstream auf `origin/main` gesetzt,
  `push.default=upstream`). Kann Git `@{push}` nicht auflösen, gilt wie heute nur der aktuelle Zweig
- **guard-env, Bash-Zweig:** Ausschlüsse (`--exclude=…`, `-g '!…'`) und das bestehende Muster laufen auf dem
  normalisierten Text. Neu: Jedes Wort mit `*`, `?`, `[` oder `{` wird mit seinem letzten Pfadteil gegen die
  Geheimnis-Namen abgeglichen, mit derselben Funktion wie heute im Zweig der Lese-Werkzeuge (Klammern eine Ebene
  tief, ohne Gross-/Kleinschreibung). Unterschied zur Shell beachtet: Ein Platzhalter am Anfang trifft keinen
  Punktnamen (`cat *`, `ls *.md` bleiben erlaubt), nur Muster, die mit `.` beginnen (`.*`, `.e*`, `{.env,x}`)
- **guard-env, Symlinks:** Jedes Wort des Befehls, das im `cwd` der Eingabe einen Symlink nennt (bei Platzhaltern
  jeder Treffer von `compgen -G`), wird mit `readlink -f` aufgelöst; heisst das Ziel wie die Geheimnis-Datei,
  lehnt der Guard ab. Im Zweig der Lese-Werkzeuge gilt das für die Pfadfelder (`file_path`, `path`, `paths`, …).
  Harte Links und Kopien erkennt der Guard nicht (Restrisiko)
- **Keine neue ADR:** ADR 0007 legt fest, dass die Guards Push und Geheimnis schützen; die Normalisierung ist die
  Umsetzung dieser Zusage. Neu ist nur das ausdrückliche Bedrohungsmodell, ein Punkt unter «Konsequenzen»
- **Gate und CI unverändert:** `scripts/gate.sh` (Schritt `guards`) und `ci-pr.yml` rufen die Testskripte schon auf

## Aufgaben

- [x] 1. `flow_shell_segments` in `lib.sh` mit Fällen in `test-hook-input.sh` — erledigt, wenn
  `bash .claude/flow/scripts/test-hook-input.sh` grün ist, mit Fällen für Fortsetzung, Anführungszeichen, Backslash
  und `$(…)`
- [x] 2. guard-push auf `flow_shell_segments` umstellen, die vier Umgehungen aus dem Issue als Fälle — erledigt,
  wenn `test-guard-push.sh` grün ist und die neuen Fälle vor der Änderung rot waren
- [x] 3. guard-push: `--all`/`--branches`, Glob-Refspec, `@{push}` (freigegeben, Entscheid 1) — erledigt,
  wenn `test-guard-push.sh` grün ist, `@{push}` geprüft in einem temporären Repository mit Upstream `origin/main`
- [x] 4. guard-env Bash-Zweig auf normalisiertem Text, Platzhalter-Wörter mit gemeinsamer Abgleich-Funktion —
  erledigt, wenn `test-guard-env.sh` grün ist und die Fälle für Platzhalter und Anführungszeichen vor der Änderung
  rot waren
- [x] 5. guard-env: Symlinks auflösen, Bash und Lese-Werkzeuge — erledigt, wenn `test-guard-env.sh` mit Fällen in
  einem temporären Ordner (Platzhalter-Geheimnis, Symlink darauf) grün ist
- [x] 6. Kopfkommentare der beiden Guards (Bedrohungsmodell, Restrisiken, Ausweg `git commit -F`) und ADR 0007 —
  erledigt, wenn `check-spelling.sh` still bleibt und der Index (`scripts/adr-index.sh`) unverändert ist
- [x] 7. Abschluss — erledigt, wenn `scripts/gate.sh --fast` grün ist (Schritt `guards` enthalten)

Aufgaben 2 und 3 sowie 4 und 5 berühren je dieselbe Datei; sie sind getrennt, damit jede für sich rot → grün geht.

## Test-Strategie

Alle Punkte auf Ebene Unit (Bash-Testskripte unter `.claude/flow/scripts/`, Ebene wie bei #24). Jeder Fall läuft wie
bisher mit drei Eingabeformaten: Claude Code, Copilot im Prompt-Modus, Copilot eigenes Format. Bei Ablehnung steht
zusätzlich das JSON mit `permissionDecision: deny` auf stdout.

- [x] Unit `flow_shell_segments`: `git push origin \` + Umbruch + `HEAD:main` ergibt ein Segment
  `git push origin HEAD:main`; `"git" push` → `git push`; `.e""nv` → `.env`; `echo $(git push x)` ergibt ein eigenes
  Segment `git push x`; ein leerer Befehl ergibt keine Zeile
- [x] Unit guard-push, abgelehnt (Exit 2): `git -c k=v push origin main`, `git --no-pager push origin main`,
  `"git" push origin main`, `git push origin \` + Umbruch + `HEAD:main`, `/usr/bin/git push origin main`,
  `git -C . -c a=b --no-pager push origin HEAD:main`, `sh -c "git push origin main"`
- [x] Unit guard-push, Zusätze (falls freigegeben): `git push --all origin` und `git push --branches origin` auf
  einem Feature-Zweig, `git push origin 'refs/heads/*:refs/heads/*'`; `git push` auf einem Feature-Zweig mit Upstream
  `origin/main` und `push.default=upstream` (temporäres Repository)
- [x] Unit guard-push, erlaubt (Exit 0, kein deny auf stdout): der Devcontainer-Push
  `git -c credential.helper= -c 'credential.https://github.com.helper=!gh auth git-credential' push -u origin feature/x`,
  `git --no-pager log`, `git push origin 'refs/tags/*'` (Glob, Ziel nicht `main`), `git push` auf einem
  Feature-Zweig ohne Upstream; alle bestehenden Fälle bleiben unverändert
- [x] Unit guard-push, bewusster Fehlalarm belegt: `git commit -m "push origin main"` → Exit 2 (fail closed, Ausweg
  `-F`), damit eine spätere Lockerung bewusst geschieht
- [x] Unit guard-env, abgelehnt: `cat .en*`, `cat .en[v]`, `cat .e?v`, `cat {.env,x}`, `cat .*`, `cat .e""nv`,
  `cat '.e'nv`, `cat .\env`, `head -n1 ./.ENV*`
- [x] Unit guard-env, erlaubt (Fehlalarme vermeiden): `cat *`, `ls *.md`, `grep -E 'foo.*bar' src`,
  `find . -name '*.ts'`, `cat env.example`, `cat .envrc`; bestehende erlaubte Fälle (`git ls-files .env`,
  `--exclude='.env*'`) unverändert
- [x] Unit guard-env, Symlink (temporärer Ordner mit Platzhalter-Geheimnis `.env` und Symlink `x` darauf):
  `cat x` → 2, `cat ./x` → 2, `cat x*` → 2, Read mit `file_path` auf `x` → 2, `view` (eigenes Format) auf `x` → 2;
  ein Symlink auf `README.md` → 0
- [x] Bestehende Tests: keine Abschwächung. Ein bestehender Fall, der durch die breitere Erkennung anders ausgeht,
  ist ein Befund für die Notizen und eine Rückfrage, keine stille Änderung
- [x] Prüfung von Hand: keine nötig. Die Guards laufen in Claude Code und Copilot über dieselben Skripte, die
  Eingabeformate sind in den Tests abgedeckt (#24)

## Entscheide (vom Nutzer bestätigt, 2026-11-08)

1. **Zusätze bei guard-push über das Issue hinaus:** `--all`/`--branches`, Glob-Refspec und Push ohne Refspec
   über einen Upstream auf `main` sind dieselbe Klasse (Ziel `main` ohne das Wort `main` an der erwarteten Stelle),
   stehen aber nicht im Issue. **Entscheid:** mitnehmen (Aufgabe 3), je ein paar Zeilen und Fälle
2. **Zweite Schicht über einen Git-Hook `pre-push`?** Er sieht das echte Ziel jeder Referenz (`<remote ref>` auf
   stdin), unabhängig davon, wie der Befehl geschrieben ist, also auch Alias, Skript und Variable. Dafür muss er
   installiert werden (`core.hooksPath` im Devcontainer), gilt auch für Menschen und lässt sich mit `--no-verify`
   oder `-c core.hooksPath=` umgehen (die guard-push dann textlich ablehnen müsste). **Entscheid:** nicht in diesem
   Issue, sondern ein eigenes Issue mit eigener ADR: Es ändert den Ablauf für Menschen und braucht eine Installation
3. **Bedrohungsmodell in ADR 0007 nachtragen** (ein Punkt unter «Konsequenzen») oder nur in den Kopfkommentaren der
   Skripte? **Entscheid:** in ADR 0007, weil die ADR die Zusage «guard-push ersetzt die Branch Protection» macht und
   dort stehen soll, wie weit sie reicht
4. **Fehlalarm bei Commit-Nachrichten:** Die breitere Erkennung lehnt `git commit -m "…push origin main…"` ab.
   Für Nachrichten über die Guards (auch die dieses Issues) heisst das `git commit -F`. **Entscheid:** so lassen
   (fail closed); die Alternative wäre eine echte Wortzerlegung mit Anführungszeichen, die `sh -c "git push …"` wieder
   durchliesse oder rekursiv zerlegen müsste
5. **Befunde der Reviewer-Runde mitnehmen** (Nachtrag unten). **Entscheid (2026-11-08):** alles mitnehmen

## Nachtrag: Reviewer-Runde (2026-11-08)

Test-Review und Sicherheits-Review in frischem Kontext fanden weitere naheliegende Umgehungen derselben Klasse (meist
schon auf `main`) und Fehlalarme. Jeder Punkt bekommt Fälle in den Testskripten (drei Eingabeformate wie bisher).

**flow_shell_segments:** `<` und `>` werden zu Leerzeichen, damit eine Umleitung nicht am Wort klebt
(`git push origin main>/dev/null`, `cat<x`, `cat 0<.en[v]`).

**guard-push, neu abgelehnt:**
- `@` gilt wie `HEAD` (`git push origin @` auf main)
- Ein Refspec mit `$` (Rest einer Ersetzung `$(…)` oder einer Variablen) gilt wie `HEAD`: auf main abgelehnt
  (`git push origin $(git branch --show-current)`, `git push -u origin "$(git rev-parse --abbrev-ref HEAD)"`), auf
  einem Feature-Zweig erlaubt. `HEAD:$(echo main)` bleibt Restrisiko (Klasse «Variable»)
- Gebündelte Kurzoptionen mit `f` oder `d` (`-fu`, `-uf`, `-ud`)
- Abgekürzte Langoptionen: ein Wort `--x…` (Teil vor `=`, mindestens drei Zeichen), das Präfix von `--force`,
  `--force-with-lease`, `--force-if-includes`, `--mirror`, `--delete`, `--all` oder `--branches` ist (`--del`,
  `--force-w`, `--al`, `--mirr`)
- Refspec `:` (matching) und ein Ziel `heads/main` (Git ergänzt zu `refs/heads/main`)
- `git-push` als Befehl (Basisname `git-push`)
- Glob-Ziel ohne `refs/heads/`, das main trifft (`m*:m*`)

**guard-push, neu erlaubt bzw. präzisiert:**
- `git stash push …` ist kein Push (auch `--all`, auch auf main)
- `@{push}` trifft nur `refs/remotes/<remote>/<main>` (Remote ohne `/`); Upstream `origin/feature/main` → erlaubt

**guard-env, neu abgelehnt:**
- Vor dem Namen jedes Zeichen ausser Buchstabe, Ziffer, `_` und `.` (`curl --data-binary @<name>`, `-F f=@<name>`,
  `$'<name>'`, `${F:-<name>}`), danach jedes ausser Buchstabe, Ziffer, `_`, `.` und `-`
- Mehrere Endungen (`.env.production.local`), in Bash und in den Lese-Werkzeugen
- Platzhalter-Wort, das `env` enthält, auch ohne Punkt am Anfang; abgeglichen wird der Teil nach dem letzten `/`
  bzw. `=` (`find . -name '*env' -exec cat {} +`, `grep -r X --include='*env*' .`). `cat *` und `ls *.md` bleiben
  erlaubt
- Klammern mehrfach und Bereiche aus einzelnen Zeichen (`.{e,}{n,}v`, `.{d..f}nv`), auch im glob-Feld des
  Grep-Werkzeugs
- Die Ausnahme `git ls-files .env` gilt nur am Anfang eines Segments (`tail -n +1 git ls-files .env` → abgelehnt)

**guard-env, erlaubt:** `git diff -- . ':!.env'` (Ausschluss als Pathspec, wie `--exclude`).

**guard-env, `cwd`:** Symlinks werden gegen das `cwd` der Eingabe aufgelöst, auch wenn der Prozess in einem anderen
Ordner läuft (Fall mit abweichendem Prozessverzeichnis).

**Restrisiken** (Kopfkommentare, `notes.md`): Pipes, die den Namen erst zur Laufzeit bilden
(`ls -a | grep env | xargs cat`, `echo main | xargs git push origin`), `-c remote.origin.push=…`, `$'main'`,
`HEAD:$(echo main)`, `cd sub && cat link` (Symlink relativ zu einem anderen Ordner).

**Bewusste Fehlalarme (fail closed), mit Fall belegt:** `git log --grep push` auf dem Zweig main (Push ohne Refspec
auf main), `grep -E '.*' README.md`, `find . -name '.*'`.

- [x] 8. Fälle zum Nachtrag (test-author), vor der Änderung rot bzw. bei Fehlalarmen rot
- [x] 9. Umsetzung, alle drei Testskripte grün, `scripts/gate.sh --fast` grün
