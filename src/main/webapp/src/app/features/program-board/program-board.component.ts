import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Board } from '../../core/models/board.model';
import { BoardMilestone } from '../../core/models/milestone.model';
import { BoardService } from '../../core/services/board.service';

/**
 * Das Program Board eines PI (US-12): Teams × Iterationen, links das nicht eingeplante Backlog, Milestones als
 * Marker im Kopf ihrer Iteration (US-15). Nur Anzeige.
 */
@Component({
  selector: 'app-program-board',
  imports: [DatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './program-board.component.html',
})
export class ProgramBoardComponent {
  protected readonly service = inject(BoardService);

  readonly id = input.required<string>();

  constructor() {
    effect(() => void this.service.load(this.id()));
  }

  /** Die Milestones über einer Iteration, in der Reihenfolge des Servers (nach Datum). */
  protected milestonesIn(board: Board, iterationNumber: number): readonly BoardMilestone[] {
    return board.milestones.filter((m) => m.iterationNumber === iterationNumber);
  }
}
