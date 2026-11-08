# Notizen US-18

- Nachtlauf: `npm ci` im frischen Worktree nötig (node_modules fehlte); lief über `npm ci --prefix`, ohne Paketänderung
- Bestehender Test (#17) «zieht nur Linien für Abhängigkeiten, deren beide Features eingeplant sind» erwartet jetzt
  `conflict: true` für Kasse (Iteration 1) braucht Portal (Iteration 3) — nach AK 1 ein Konflikt; die übrigen
  #17-Erwartungen tragen `conflict: false`. Grund: Delta US-18 → AK 1/3
- Annahme: Fehlt eine Iteration im Board (nur in Testdaten möglich), steht im Text die Nummer statt des Namens
- Annahme: Eine hervorgehobene Konfliktlinie bleibt rot und gestrichelt (kein `text-accent`); abgeblendet wird sie
  wie jede andere Linie
- Annahme: Titel werden wie `insertByRank` ohne Gross-/Kleinschreibung verglichen (`sensitivity: 'base'`)
- Annahme: Der Abschnitt «Konflikte» erscheint, sobald das Board geladen ist, auch ohne Teams (dann «Keine
  Konflikte»); keine Sonderregel
- Backlog-Karten bekommen `lacksDelivery` nicht: Der Hinweis gilt nur für eingeplanten Bedarf
- Eigener Pfeil-Marker `dependency-arrow-conflict` mit `text-danger`, weil ein SVG-Marker die Farbe des Pfads nicht
  erbt; nur auf dem Screenshot prüfbar
