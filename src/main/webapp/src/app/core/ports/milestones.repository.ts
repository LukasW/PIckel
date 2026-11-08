import { Milestone, MilestoneInput } from '../models/milestone.model';

/** Driven Port: woher die Milestones eines PI kommen (heute REST). */
export abstract class MilestonesRepository {
  /** Aufsteigend nach Datum. */
  abstract list(programIncrementId: string): Promise<Milestone[]>;
  abstract add(programIncrementId: string, input: MilestoneInput): Promise<Milestone>;
  abstract update(
    programIncrementId: string,
    milestoneId: string,
    input: MilestoneInput,
  ): Promise<Milestone>;
  abstract remove(programIncrementId: string, milestoneId: string): Promise<void>;
}
