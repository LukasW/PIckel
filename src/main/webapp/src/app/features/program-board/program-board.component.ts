import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BoardService } from '../../core/services/board.service';

/** Das Program Board eines PI (US-12): Teams × Iterationen, links das nicht eingeplante Backlog. Nur Anzeige. */
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
}
