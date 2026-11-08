import {
  BusinessContext,
  NewPiLink,
  PlanProgramIncrement,
  ProgramIncrement,
} from '../models/program-increment.model';

/** Driven Port: woher die PIs kommen (heute REST, später ggf. offline-fähig). */
export abstract class ProgramIncrementsRepository {
  abstract list(): Promise<ProgramIncrement[]>;
  abstract get(id: string): Promise<ProgramIncrement>;
  abstract plan(request: PlanProgramIncrement): Promise<ProgramIncrement>;
  abstract updateBusinessContext(id: string, request: BusinessContext): Promise<ProgramIncrement>;
  abstract addLink(id: string, request: NewPiLink): Promise<ProgramIncrement>;
  abstract removeLink(id: string, linkId: string): Promise<ProgramIncrement>;
}
