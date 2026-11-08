/** Ein Termin im PI (Release, Messe, Audit); `date` als `YYYY-MM-DD` im Zeitraum des PI. */
export interface Milestone {
  readonly id: string;
  readonly name: string;
  readonly date: string;
}

/** Ein Milestone auf dem Board: der Marker steht über der Iteration mit dieser Nummer. */
export interface BoardMilestone extends Milestone {
  readonly iterationNumber: number;
}

/** Eingabe zum Erfassen und Ändern. */
export interface MilestoneInput {
  readonly name: string;
  readonly date: string;
}
