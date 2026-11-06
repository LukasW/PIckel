import { PlanProgramIncrement, ProgramIncrement } from '../models/program-increment.model';

/** Driven Port: woher die PIs kommen (heute REST, später ggf. offline-fähig). */
export abstract class ProgramIncrementsRepository {
  abstract list(): Promise<ProgramIncrement[]>;
  abstract plan(request: PlanProgramIncrement): Promise<ProgramIncrement>;
}
