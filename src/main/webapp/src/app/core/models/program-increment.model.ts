/** Eine Iteration eines PI; vom Backend berechnet. Datumswerte als ISO-Datum (YYYY-MM-DD). */
export interface Iteration {
  readonly number: number;
  readonly name: string;
  readonly startDate: string;
  readonly endDate: string;
  readonly innovationAndPlanning: boolean;
}

/** Ein Program Increment; Datumswerte als ISO-Datum (YYYY-MM-DD). */
export interface ProgramIncrement {
  readonly id: string;
  readonly name: string;
  readonly startDate: string;
  readonly endDate: string;
  readonly iterationLengthWeeks: number;
  readonly iterations: readonly Iteration[];
}

export interface PlanProgramIncrement {
  readonly name: string;
  readonly startDate: string;
  readonly endDate: string;
  readonly iterationLengthWeeks?: number;
}
