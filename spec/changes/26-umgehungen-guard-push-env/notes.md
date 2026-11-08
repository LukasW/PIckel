# Notizen — Issue #26

## Annahmen (Lücken, die `test-author` gemeldet hat; klein, selbst entschieden)

- `flow_shell_segments` endet immer mit Exit 0, auch beim leeren Befehl; sonst wäre «keine Zeile» nicht von einem
  Fehler zu unterscheiden. Festgehalten in `test-hook-input.sh` (leere Eingaben)
- Segmente werden am Rand getrimmt, Segmente nur aus Leerzeichen fallen weg. Festgehalten in `test-hook-input.sh`
- `git push origin` ohne Refspec zählt wie `git push` (Prüfung über `@{push}`). Festgehalten in `test-guard-push.sh`
- Relative Pfade in den Pfadfeldern der Lese-Werkzeuge und in Bash werden gegen das `cwd` der Eingabe aufgelöst,
  ohne `cwd` gegen das aktuelle Verzeichnis (wie `flow_tool_files`). Festgehalten in `test-guard-env.sh` (Read mit
  relativem `file_path` auf den Symlink)

## Umsetzung

- Der Heredoc für das Patch-Skript von `guard-env.sh` wurde von `guard-env` selbst abgelehnt, weil er den
  Dateinamen nennt. Das ist das dokumentierte Verhalten (Ausweg: Text mit Write schreiben); es zeigt, dass der
  Bash-Zweig nach der Umstellung weiter greift
- `guard-push` prüft Glob-Refspecs gegen `refs/heads/<main>` und `<main>`; ein Ziel nur `*` trifft damit ebenfalls
- Bewusst in Kauf genommene Fehlalarme (fail closed): `git log --grep push` auf `main` (Wort `push` nach `git`,
  ohne Refspec auf main) und `grep -E '.*' datei` (Wort `.*` mit Punkt am Anfang). Ausweg: Suchtext anders
  schreiben bzw. `-F`/`-f <datei>`
- Reviewer-Runde: Der Sicherheits-Review fand weitere Umgehungen derselben Klasse, meist schon auf `main`
  (`git push origin @`, `$(git branch --show-current)` auf main, `-fu`, `--del`, `curl @<name>`, `*env` mit find,
  Klammern, mehrfache Endungen) und zwei neue Fehlalarme (`git stash push`). Vom Nutzer als Entscheid 5 freigegeben,
  im Plan unter «Nachtrag» spezifiziert, vom `test-author` mit Fällen belegt
- Annahme: Ein Refspec mit `$` gilt wie `HEAD` (auf main abgelehnt, auf Feature-Zweigen erlaubt); `$BRANCH` zählt
  dazu. `HEAD:$(echo main)` bleibt Restrisiko
- Annahme: Abgekürzte Langoptionen zählen ab drei Zeichen samt `--` (`--a` wird abgelehnt, Git meldet es ohnehin als
  mehrdeutig)
- Brace-Auflösung in `glob_hits_env` ist auf 256 Kandidaten begrenzt; ein Muster mit mehr bleibt teilweise
  ungeprüft (theoretisch, kein naheliegender Fall)
- Die Symlink-Prüfung löst jedes Wort auf, das im `cwd` existiert, nicht nur Symlinks; damit fallen auch
  Ordner-Symlinks (`link/…`) auf
