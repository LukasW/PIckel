/** Eine Iteration eines PI; vom Backend berechnet. Datumswerte als ISO-Datum (YYYY-MM-DD). */
export interface Iteration {
  readonly number: number;
  readonly name: string;
  readonly startDate: string;
  readonly endDate: string;
  readonly innovationAndPlanning: boolean;
}

/** Ein Link am PI (US-8); das Backend lässt nur http und https zu. */
export interface PiLink {
  readonly id: string;
  readonly title: string;
  readonly url: string;
}

/** Ein Program Increment; Datumswerte als ISO-Datum (YYYY-MM-DD). */
export interface ProgramIncrement {
  readonly id: string;
  readonly name: string;
  readonly startDate: string;
  readonly endDate: string;
  readonly iterationLengthWeeks: number;
  readonly iterations: readonly Iteration[];
  readonly businessContext: string;
  readonly artObjectives: string;
  readonly links: readonly PiLink[];
}

export interface BusinessContext {
  readonly businessContext: string;
  readonly artObjectives: string;
}

export interface NewPiLink {
  readonly title: string;
  readonly url: string;
}

export interface PlanProgramIncrement {
  readonly name: string;
  readonly startDate: string;
  readonly endDate: string;
  readonly iterationLengthWeeks?: number;
}
