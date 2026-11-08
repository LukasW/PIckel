import { CdkDrag, CdkDragDrop, CdkDropList, CdkDropListGroup } from '@angular/cdk/drag-drop';
import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Board } from '../../core/models/board.model';
import { Feature } from '../../core/models/feature.model';
import { BoardMilestone } from '../../core/models/milestone.model';
import { BoardService } from '../../core/services/board.service';
import { BoardCardComponent, SchedulingChoice } from './board-card.component';

/** Ziel eines Ablegens: das Backlog oder eine Zelle Team × Iteration. */
type DropTarget = 'backlog' | SchedulingChoice;

/**
 * Das Program Board eines PI (US-12): Teams × Iterationen, links das nicht eingeplante Backlog, Milestones als
 * Marker im Kopf ihrer Iteration (US-15). Karten lassen sich in Zellen ziehen und einplanen (US-13).
 */
@Component({
  selector: 'app-program-board',
  imports: [BoardCardComponent, CdkDrag, CdkDropList, CdkDropListGroup, DatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './program-board.component.html',
})
export class ProgramBoardComponent {
  protected readonly service = inject(BoardService);

  readonly id = input.required<string>();

  protected readonly backlogTarget: DropTarget = 'backlog';

  constructor() {
    effect(() => void this.service.load(this.id()));
  }

  /** Die Milestones über einer Iteration, in der Reihenfolge des Servers (nach Datum). */
  protected milestonesIn(board: Board, iterationNumber: number): readonly BoardMilestone[] {
    return board.milestones.filter((m) => m.iterationNumber === iterationNumber);
  }

  /** Die Karten einer Zelle, in der Reihenfolge des Servers (Backlog-Reihenfolge). */
  protected cardsIn(board: Board, teamId: string, iterationNumber: number): readonly Feature[] {
    return board.scheduled.filter(
      (f) => f.teamId === teamId && f.iterationNumber === iterationNumber,
    );
  }

  protected cell(teamId: string, iterationNumber: number): DropTarget {
    return { teamId, iterationNumber };
  }

  protected drop(event: CdkDragDrop<DropTarget>): void {
    if (event.previousContainer === event.container) {
      return;
    }
    const feature = event.item.data as Feature;
    const target = event.container.data;
    if (target === 'backlog') {
      void this.service.unschedule(feature);
    } else {
      void this.service.schedule(feature, target.teamId, target.iterationNumber);
    }
  }
}
