import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { Feature } from '../../core/models/feature.model';
import { Iteration } from '../../core/models/program-increment.model';
import { Team } from '../../core/models/team.model';

/** Die Zelle, in die eine Karte eingeplant werden soll. */
export interface SchedulingChoice {
  teamId: string;
  iterationNumber: number;
}

/**
 * Eine Feature-Karte auf dem Program Board (US-13). Neben dem Ziehen bietet sie die Aktion «Einplanen …» mit einem
 * Inline-Formular, damit sich jede Karte auch ohne Maus einplanen, verschieben und zurücknehmen lässt.
 */
@Component({
  selector: 'app-board-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './board-card.component.html',
})
export class BoardCardComponent {
  readonly feature = input.required<Feature>();
  readonly teams = input.required<readonly Team[]>();
  readonly iterations = input.required<readonly Iteration[]>();

  readonly schedule = output<SchedulingChoice>();
  readonly unschedule = output<void>();

  protected readonly open = signal(false);
  // Eine eingeplante Karte startet mit ihrer Zelle; eine Backlog-Karte ohne Auswahl.
  protected readonly teamId = signal('');
  protected readonly iterationNumber = signal('');

  /** Die IP-Iteration wird nicht angeboten: Dort wird keine Feature-Arbeit geplant. */
  protected readonly developmentIterations = computed(() =>
    this.iterations().filter((iteration) => !iteration.innovationAndPlanning),
  );

  protected readonly complete = computed(
    () => this.teamId() !== '' && this.iterationNumber() !== '',
  );

  protected valueOf(event: Event): string {
    return (event.target as HTMLSelectElement).value;
  }

  protected openForm(): void {
    const { teamId, iterationNumber } = this.feature();
    this.teamId.set(teamId ?? '');
    this.iterationNumber.set(iterationNumber === undefined ? '' : String(iterationNumber));
    this.open.set(true);
  }

  protected submit(): void {
    if (!this.complete()) {
      return;
    }
    this.schedule.emit({ teamId: this.teamId(), iterationNumber: Number(this.iterationNumber()) });
    this.open.set(false);
  }

  protected takeBack(): void {
    this.unschedule.emit();
    this.open.set(false);
  }
}
