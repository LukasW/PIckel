import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProgramIncrement } from '../../core/models/program-increment.model';
import { ProgramIncrementsService } from '../../core/services/program-increments.service';

/** Detailseite eines PI mit seinen Iterationen (US-5); die ID kommt aus der Route. */
@Component({
  selector: 'app-program-increment-detail',
  imports: [DatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './program-increment-detail.component.html',
})
export class ProgramIncrementDetailComponent implements OnInit {
  protected readonly service = inject(ProgramIncrementsService);

  readonly id = input.required<string>();
  protected readonly programIncrement = signal<ProgramIncrement | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.programIncrement.set(await this.service.get(this.id()));
  }
}
