# language: de
Funktionalität: US-18 Hinweis bei Konflikt einer Abhängigkeit

  Als RTE
  möchte ich sehen, wenn ein Feature später geliefert wird, als es gebraucht wird
  damit der Konflikt noch im Planning gelöst wird

  # Vitest: src/main/webapp/src/app/core/services/board.service.spec.ts → meldet einen Konflikt, wenn Login in Iteration 4 geliefert wird und Portal es in Iteration 2 braucht (US-18)
  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → nennt in der Liste «Konflikte», dass Login in Iteration 4 zu spät für Portal in Iteration 2 kommt (US-18)
  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → markiert die Linie von Login zu Portal als Konflikt: rot und gestrichelt (US-18)
  @Unit
  Szenario: Lieferung nach dem Bedarf ist ein Konflikt
    Gegeben sei ein Board mit "Portal" bei "Adler" in "Iteration 2" und "Login" bei "Zebra" in "Iteration 4"
    Und "Portal" braucht "Login"
    Wenn ich das Board ansehe
    Dann nennt die Liste «Konflikte» "Login wird in Iteration 4 geliefert, Portal braucht es in Iteration 2."
    Und ist die Linie von "Login" zu "Portal" als Konflikt markiert

  # Vitest: src/main/webapp/src/app/core/services/board.service.spec.ts → meldet keinen Konflikt, wenn Login in derselben Iteration geliefert wird, wie Portal es braucht (US-18)
  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → zeigt «Keine Konflikte», wenn Login in derselben Iteration geliefert wird, wie Portal es braucht (US-18)
  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → markiert die Linie von Login zu Portal nicht, wenn Login in derselben Iteration kommt (US-18)
  @Unit
  Szenario: Lieferung in derselben Iteration ist kein Konflikt
    Gegeben sei ein Board mit "Portal" bei "Adler" in "Iteration 2" und "Login" bei "Zebra" in "Iteration 2"
    Und "Portal" braucht "Login"
    Wenn ich das Board ansehe
    Dann zeigt die Liste «Konflikte» "Keine Konflikte"
    Und ist die Linie von "Login" zu "Portal" nicht als Konflikt markiert

  # Vitest: src/main/webapp/src/app/core/services/board.service.spec.ts → meldet «Lieferung nicht eingeplant», wenn Portal in Iteration 2 eingeplant ist und Login im Backlog liegt (US-18)
  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → nennt in der Liste, dass Login nicht eingeplant ist, und Portal trägt «Lieferung nicht eingeplant» (US-18)
  # Vitest: src/main/webapp/src/app/features/program-board/board-card.component.spec.ts → trägt den Hinweis «Lieferung nicht eingeplant», wenn dem Feature eine Lieferung fehlt (US-18)
  @Unit
  Szenario: Nicht eingeplante Lieferung wird gemeldet
    Gegeben sei ein Board mit "Portal" bei "Adler" in "Iteration 2" und "Login" im Backlog
    Und "Portal" braucht "Login"
    Wenn ich das Board ansehe
    Dann nennt die Liste «Konflikte» "Login ist nicht eingeplant, Portal braucht es in Iteration 2."
    Und trägt die Karte "Portal" den Hinweis "Lieferung nicht eingeplant"

  # Vitest: src/main/webapp/src/app/core/services/board.service.spec.ts → löst den Konflikt sofort, wenn Login in Iteration 1 verschoben wird, ohne das Board neu zu laden (US-18)
  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → zeigt «Keine Konflikte», nachdem Login per Aktion in Iteration 1 eingeplant ist, ohne Neuladen (US-18)
  @Unit
  Szenario: Verschieben löst den Konflikt
    Gegeben sei ein Board mit "Portal" bei "Adler" in "Iteration 2" und "Login" bei "Zebra" in "Iteration 4"
    Und "Portal" braucht "Login"
    Wenn ich "Login" bei "Zebra" in "Iteration 1" einplane
    Dann zeigt die Liste «Konflikte» "Keine Konflikte"

  # Vitest: src/main/webapp/src/app/core/services/board.service.spec.ts → stellt den Konflikt wieder her, wenn das Verschieben von Login scheitert (US-18)
  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → nennt wieder den Konflikt, wenn das Speichern beim Einplanen von Login scheitert (US-18)
  @Unit
  Szenario: Gescheitertes Speichern stellt den Konflikt wieder her
    Gegeben sei ein Board mit "Portal" bei "Adler" in "Iteration 2" und "Login" bei "Zebra" in "Iteration 4"
    Und "Portal" braucht "Login"
    Und das Speichern scheitert
    Wenn ich "Login" bei "Zebra" in "Iteration 1" einplane
    Dann nennt die Liste «Konflikte» "Login wird in Iteration 4 geliefert, Portal braucht es in Iteration 2."
