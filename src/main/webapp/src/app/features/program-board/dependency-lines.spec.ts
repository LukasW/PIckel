import { emphasisOf, lineBetween, Rect } from './dependency-lines';

function rect(x: number, y: number, width = 100, height = 40): Rect {
  return { x, y, width, height };
}

// Feature-IDs wie im Szenario «Hover hebt die Linien einer Karte hervor»: Portal und Suche brauchen Login.
const PORTAL = 'f1';
const LOGIN = 'f2';
const SUCHE = 'f3';
const LOGIN_TO_PORTAL = { neededFeatureId: LOGIN, needingFeatureId: PORTAL };
const LOGIN_TO_SUCHE = { neededFeatureId: LOGIN, needingFeatureId: SUCHE };

/** Die Linie führt von der liefernden (`needed`) zur brauchenden Karte (`needing`), Plan → Test-Strategie. */
describe('lineBetween', () => {
  it('führt von der rechten Kante der Lieferung zur linken Kante des Bedarfs, wenn der Bedarf rechts liegt (US-17)', () => {
    expect(lineBetween(rect(0, 0), rect(300, 100))).toEqual({
      from: { x: 100, y: 20 },
      to: { x: 300, y: 120 },
    });
  });

  it('führt von der linken Kante der Lieferung zur rechten Kante des Bedarfs, wenn die Lieferung rechts liegt (US-17)', () => {
    expect(lineBetween(rect(300, 0), rect(0, 100))).toEqual({
      from: { x: 300, y: 20 },
      to: { x: 100, y: 120 },
    });
  });

  // Annahme: Bei oberer und unterer Kante setzt die Linie wie bei den Seitenkanten in der Mitte der Kante an.
  it('führt in derselben Spalte von der unteren Kante der Lieferung zur oberen Kante des Bedarfs (US-17)', () => {
    expect(lineBetween(rect(0, 0), rect(0, 100))).toEqual({
      from: { x: 50, y: 40 },
      to: { x: 50, y: 100 },
    });
  });

  it('führt in derselben Spalte von der oberen Kante der Lieferung zur unteren Kante des Bedarfs, wenn der Bedarf darüber liegt (US-17)', () => {
    expect(lineBetween(rect(0, 100), rect(0, 0))).toEqual({
      from: { x: 50, y: 100 },
      to: { x: 50, y: 40 },
    });
  });

  it('gilt bei teilweise waagrecht überlappenden Karten als dieselbe Spalte (US-17)', () => {
    expect(lineBetween(rect(0, 0), rect(50, 100))).toEqual({
      from: { x: 50, y: 40 },
      to: { x: 100, y: 100 },
    });
  });

  it('führt bei Karten, die sich nur an der Kante berühren, von Seitenkante zu Seitenkante (US-17)', () => {
    expect(lineBetween(rect(0, 0), rect(100, 100))).toEqual({
      from: { x: 100, y: 20 },
      to: { x: 100, y: 120 },
    });
  });

  it('zeichnet keine Linie, wenn die Karte der Lieferung nicht gemessen ist (US-17)', () => {
    expect(lineBetween(undefined, rect(300, 100))).toBeUndefined();
  });

  it('zeichnet keine Linie, wenn die Karte des Bedarfs nicht gemessen ist (US-17)', () => {
    expect(lineBetween(rect(0, 0), undefined)).toBeUndefined();
  });

  // Annahme (Auftrag): Gemessen ist gemessen, auch mit 0 × 0 (jsdom misst kein Layout). Die Komponententests
  // verlassen sich darauf, dass der Pfad dann trotzdem erscheint.
  it('zeichnet eine Linie auch zwischen zwei Karten, die 0 × 0 gemessen sind (US-17)', () => {
    expect(lineBetween(rect(0, 0, 0, 0), rect(0, 0, 0, 0))).toEqual({
      from: { x: 0, y: 0 },
      to: { x: 0, y: 0 },
    });
  });
});

describe('emphasisOf', () => {
  it('zeigt ohne fokussierte Karte alle Linien normal (US-17)', () => {
    expect(emphasisOf(LOGIN_TO_PORTAL, undefined)).toBe('normal');
    expect(emphasisOf(LOGIN_TO_SUCHE, undefined)).toBe('normal');
  });

  it('hebt bei Fokus auf «Suche» die Linie Login → Suche hervor und blendet Login → Portal ab (US-17)', () => {
    expect(emphasisOf(LOGIN_TO_SUCHE, SUCHE)).toBe('highlighted');
    expect(emphasisOf(LOGIN_TO_PORTAL, SUCHE)).toBe('dimmed');
  });

  it('hebt bei Fokus auf «Portal» die Linie Login → Portal hervor und blendet Login → Suche ab (US-17)', () => {
    expect(emphasisOf(LOGIN_TO_PORTAL, PORTAL)).toBe('highlighted');
    expect(emphasisOf(LOGIN_TO_SUCHE, PORTAL)).toBe('dimmed');
  });

  it('hebt bei Fokus auf die liefernde Karte «Login» beide Linien hervor (US-17)', () => {
    expect(emphasisOf(LOGIN_TO_PORTAL, LOGIN)).toBe('highlighted');
    expect(emphasisOf(LOGIN_TO_SUCHE, LOGIN)).toBe('highlighted');
  });

  it('blendet alle Linien ab, wenn die fokussierte Karte keine Linie hat (US-17)', () => {
    expect(emphasisOf(LOGIN_TO_PORTAL, 'f9')).toBe('dimmed');
    expect(emphasisOf(LOGIN_TO_SUCHE, 'f9')).toBe('dimmed');
  });
});
