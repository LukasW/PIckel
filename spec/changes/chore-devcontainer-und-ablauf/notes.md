# Notizen

- Push über SSH scheitert im Devcontainer, weil der weitergereichte SSH-Agent keinen Schlüssel für GitHub hat. Der
  Credential-Helper von `gh` für HTTPS ist schon global eingerichtet; `gh auth setup-git` brächte nichts. Ob `origin`
  auf HTTPS umgestellt wird, entscheidet der Nutzer, nicht dieser Chore.
- Erster Versuch `host.override=…` in `~/.testcontainers.properties` wirkte nicht (CucumberIT scheiterte weiter);
  deshalb die Umgebungsvariable in `containerEnv`. Sie gilt auch für Tests aus der IDE.
- Bei nativem Docker unter Linux gibt es `host.docker.internal` im Container nicht ohne
  `--add-host=host.docker.internal:host-gateway`; dort müsste man das nachrüsten. Heute laufen Docker Desktop und
  Podman, beide stellen den Namen bereit.
- Vorschlag D ist eine Textregel und deshalb vorläufig: Streichen, wenn die ArchUnit-Regel und der Spec-Reviewer die
  Fälle ohnehin fangen.
