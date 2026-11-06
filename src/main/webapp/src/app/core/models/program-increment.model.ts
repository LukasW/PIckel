/** Ein Program Increment; Datumswerte als ISO-Datum (YYYY-MM-DD). */
export interface ProgramIncrement {
  readonly id: string;
  readonly name: string;
  readonly startDate: string;
  readonly endDate: string;
}

export interface PlanProgramIncrement {
  readonly name: string;
  readonly startDate: string;
  readonly endDate: string;
}
