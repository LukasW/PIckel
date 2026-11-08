# Notizen — Task #40

- Der anonyme Download antwortet 504 auch auf die neue Adresse `github.com/gitleaks/gitleaks/…`; `github.com`
  selbst 200, `gh api …/releases/assets/<id>` liefert das Asset. Im Browser geht es, weil dort angemeldet
- Erster lokaler Versuch mit dem x64-Binary stürzte im arm64-Devcontainer ab: deshalb wählt das Skript die Plattform
  aus `uname`
- `exec` ersetzt die Shell, ein EXIT-Trap liefe nicht: das temporäre Verzeichnis wird vor `exec` gelöscht
- Prüfung von Hand (2026-11-08, arm64): frische Ablage Exit 0 «no leaks found»; zweiter Lauf `version` → 8.24.3 ohne
  Download; Prüfsumme verfälscht → Abbruch, Exit 1, keine Ablage; Wegwerf-Repository mit GitHub-Token → «leaks
  found: 1», Exit 1
- Die Prüfsummen stammen aus `gitleaks_8.24.3_checksums.txt` des Releases; die arm64-Summe stimmt mit dem über die
  API geladenen Archiv überein
