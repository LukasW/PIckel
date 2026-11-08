/** Rechteck einer Karte, relativ zum Raster (US-17). */
export interface Rect {
  readonly x: number;
  readonly y: number;
  readonly width: number;
  readonly height: number;
}

export interface Point {
  readonly x: number;
  readonly y: number;
}

export interface LineGeometry {
  readonly from: Point;
  readonly to: Point;
}

export type Emphasis = 'normal' | 'highlighted' | 'dimmed';

/**
 * Strecke von der liefernden zur brauchenden Karte, jeweils ab der Mitte der zugewandten Kante; ohne eines der
 * Rechtecke (Karte nicht gemessen) gibt es keine Linie. Überlappen sich die Karten waagrecht (dieselbe Spalte),
 * verbindet die Linie untere und obere Kante, sonst die Seitenkanten.
 */
export function lineBetween(
  needed: Rect | undefined,
  needing: Rect | undefined,
): LineGeometry | undefined {
  if (!needed || !needing) {
    return undefined;
  }
  // Berühren sich die Karten nur an der Kante, stehen sie nebeneinander: Seitenkanten.
  const sameColumn = needed.x < right(needing) && needing.x < right(needed);
  if (sameColumn) {
    const below = needing.y >= needed.y;
    return {
      from: { x: centerX(needed), y: below ? bottom(needed) : needed.y },
      to: { x: centerX(needing), y: below ? needing.y : bottom(needing) },
    };
  }
  const toTheRight = needing.x > needed.x;
  return {
    from: { x: toTheRight ? right(needed) : needed.x, y: centerY(needed) },
    to: { x: toTheRight ? needing.x : right(needing), y: centerY(needing) },
  };
}

/**
 * Hervorhebung einer Linie: Ohne fokussierte Karte sind alle normal; sonst sind die Linien der fokussierten Karte
 * hervorgehoben und alle übrigen abgeblendet, damit das Board bei vielen Linien lesbar bleibt.
 */
export function emphasisOf(
  link: { neededFeatureId: string; needingFeatureId: string },
  focusedFeatureId: string | undefined,
): Emphasis {
  if (focusedFeatureId === undefined) {
    return 'normal';
  }
  return link.neededFeatureId === focusedFeatureId || link.needingFeatureId === focusedFeatureId
    ? 'highlighted'
    : 'dimmed';
}

function right(rect: Rect): number {
  return rect.x + rect.width;
}

function bottom(rect: Rect): number {
  return rect.y + rect.height;
}

function centerX(rect: Rect): number {
  return rect.x + rect.width / 2;
}

function centerY(rect: Rect): number {
  return rect.y + rect.height / 2;
}
