/** Kapazität eines Teams in einer Entwicklungs-Iteration eines PI, in Story Points; ohne Eintrag 0 (US-7). */
export interface Capacity {
  readonly teamId: string;
  readonly iterationNumber: number;
  readonly points: number;
}
