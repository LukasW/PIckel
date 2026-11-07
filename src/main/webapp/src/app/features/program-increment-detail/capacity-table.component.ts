import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { Iteration, ProgramIncrement } from '../../core/models/program-increment.model';
import { Team } from '../../core/models/team.model';
import { CapacitiesService } from '../../core/services/capacities.service';
import { TeamsService } from '../../core/services/teams.service';

/** Wie die Meldung der Domain im Backend; das Formular meldet es nur früher. */
const INVALID_POINTS = 'Die Kapazität muss eine ganze Zahl ab 0 sein.';

/** Tabelle Teams × Entwicklungs-Iterationen mit der Kapazität in Story Points (US-7). */
@Component({
  selector: 'app-capacity-table',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './capacity-table.component.html',
})
export class CapacityTableComponent {
  protected readonly teamsService = inject(TeamsService);
  protected readonly capacitiesService = inject(CapacitiesService);

  readonly programIncrement = input.required<ProgramIncrement>();

  /** In der IP-Iteration wird keine Kapazität geplant (Delta US-7). */
  protected readonly iterations = computed(() =>
    this.programIncrement().iterations.filter((iteration) => !iteration.innovationAndPlanning),
  );

  /** Meldung des Formulars, bevor ein Wert an den Server geht. */
  private readonly inputError = signal<string | undefined>(undefined);

  protected readonly error = computed(
    () => this.inputError() ?? this.capacitiesService.error() ?? this.teamsService.error(),
  );

  /** Punkte je Zelle, Schlüssel `<teamId>:<iterationNumber>`. */
  private readonly pointsByCell = computed(
    () =>
      new Map(
        this.capacitiesService
          .capacities()
          .map((c) => [cellKey(c.teamId, c.iterationNumber), c.points]),
      ),
  );

  constructor() {
    void this.teamsService.load();
    effect(() => void this.capacitiesService.load(this.programIncrement().id));
  }

  // Ohne Wert vom Server gilt 0, wie im Backend; ein leeres Feld wäre in der Tabelle irreführend.
  protected points(team: Team, iteration: Iteration): number {
    return this.pointsByCell().get(cellKey(team.id, iteration.number)) ?? 0;
  }

  protected async save(team: Team, iteration: Iteration, event: Event): Promise<void> {
    const target = event.target as HTMLInputElement;
    const stored = this.points(team, iteration);
    this.inputError.set(undefined);
    // Ein geleertes Feld speichert nichts. Text wie «abc» liefert der Browser ebenfalls leer, aber mit `badInput`.
    if (target.value.trim() === '') {
      if (target.validity?.badInput) {
        this.inputError.set(INVALID_POINTS);
      }
      target.value = String(stored);
      return;
    }
    const points = Number(target.value);
    if (!Number.isInteger(points) || points < 0) {
      this.inputError.set(INVALID_POINTS);
      target.value = String(stored);
      return;
    }
    const saved = await this.capacitiesService.set(
      this.programIncrement().id,
      team.id,
      iteration.number,
      points,
    );
    if (!saved) {
      target.value = String(stored);
    }
  }
}

function cellKey(teamId: string, iterationNumber: number): string {
  return `${teamId}:${iterationNumber}`;
}
