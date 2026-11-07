import { Team } from '../models/team.model';

/** Driven Port: woher die Teams kommen (heute REST). */
export abstract class TeamsRepository {
  abstract list(): Promise<Team[]>;
  abstract add(name: string): Promise<Team>;
  abstract rename(id: string, name: string): Promise<Team>;
  abstract remove(id: string): Promise<void>;
}
