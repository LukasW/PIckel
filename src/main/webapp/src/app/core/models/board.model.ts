import { Capacity } from './capacity.model';
import { Feature } from './feature.model';
import { BoardMilestone } from './milestone.model';
import { Iteration } from './program-increment.model';
import { Team } from './team.model';

/** Das Program Board eines PI: Teams als Zeilen, Iterationen als Spalten, dazu das nicht eingeplante Backlog und die Eingeplanten. */
export interface Board {
  readonly teams: readonly Team[];
  readonly iterations: readonly Iteration[];
  readonly backlog: readonly Feature[];
  /** Eingeplante Features in Backlog-Reihenfolge; die Zelle steht am Feature (`teamId`, `iterationNumber`). */
  readonly scheduled: readonly Feature[];
  /** Aufsteigend nach Datum; je ein Marker über der Iteration mit `iterationNumber`. */
  readonly milestones: readonly BoardMilestone[];
  /** Verfügbare Kapazität je Team und Entwicklungs-Iteration (ohne Eintrag 0); keine für die IP-Iteration. */
  readonly capacities: readonly Capacity[];
}

/** Last einer Zelle Team × Iteration in Story Points; `overloaded`, wenn geplant grösser als verfügbar ist. */
export interface CellLoad {
  readonly planned: number;
  readonly available: number;
  readonly overloaded: boolean;
}
