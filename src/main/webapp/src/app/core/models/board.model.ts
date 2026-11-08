import { FeatureDependency } from './feature-dependency.model';
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
  /** Alle Abhängigkeiten des PI (US-17); welche eine Linie bekommt, leitet der `BoardService` ab. */
  readonly dependencies: readonly FeatureDependency[];
}

/** Eine Linie auf dem Board: von der liefernden zur brauchenden Karte (US-17). */
export interface DependencyLink {
  readonly neededFeatureId: string;
  readonly needingFeatureId: string;
}
