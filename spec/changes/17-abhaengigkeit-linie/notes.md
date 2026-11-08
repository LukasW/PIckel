# Notizen US-17

- Reihenfolge: #14 ist noch nicht gemergt; #17 hängt fachlich nur von #13 und #16 ab (beide gemergt). Konflikte mit
  #14 in `ProgramBoard`, `BoardDto`, `board.model.ts` und den Fixtures sind möglich, aber rein mechanisch
- Annahme (Nachtlauf): Fokus/Hover auf einer Karte ohne Linie (etwa im Backlog) blendet alle Linien ab — wörtlich
  aus AK 4 («blendet die übrigen ab»). Test: `dependency-lines.spec.ts` → «blendet alle Linien ab, wenn die
  fokussierte Karte keine Linie hat (US-17)»
- Annahme: «Dieselbe Spalte» heisst waagrecht überlappende Karten; die Linie setzt dann in der Mitte der unteren bzw.
  oberen Kante an, sonst in der Mitte der Seitenkanten. Test: `dependency-lines.spec.ts` → `lineBetween`
- Annahme: Liegen beide Features im Backlog, zeigen beide Karten die Abhängigkeit im Zähler (aus AK 3 gefolgert).
  Test: `board.service.spec.ts` → «zählt die Abhängigkeit an beiden Karten, wenn beide im Backlog liegen (US-17)»
- Annahme: Eine gemessene Karte gilt als gemessen, auch mit 0 × 0 (jsdom); sonst zeichnet die Komponente ohne
  Layout keine Linie. Test: «zeichnet eine Linie auch zwischen zwei Karten, die 0 × 0 gemessen sind (US-17)»
- Messung: Wrapper `div.relative.min-w-max` um Tabelle und SVG, damit die Linien mit dem Raster scrollen;
  `min-w-max` wanderte dafür von der Tabelle auf den Wrapper (gleiches Layout)
- Test-Review: Karten, die sich nur an der Kante berühren, gelten als nebeneinander (Seitenkanten); teilweise
  überlappende als dieselbe Spalte. Je ein Test in `dependency-lines.spec.ts`
- `focusout` innerhalb derselben Karte (Fokus wandert ins Formular) hebt die Hervorhebung nicht auf
- Ohne `ResizeObserver` (jsdom) misst die Komponente nur nach dem Rendern
- Die Schätzung der Story ist offen (`—`): Weder Issue noch Change-Ordner nennen eine
- Der Screenshot (Plan → Test-Strategie «Von Hand») fehlt im Nachtlauf: kein Browser in der Umgebung
