import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  OnInit,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProgramIncrement } from '../../core/models/program-increment.model';
import { ProgramIncrementsService } from '../../core/services/program-increments.service';
import { notBlank } from '../../shared/validators/text.validators';
import { FeatureBacklogComponent } from '../feature-backlog/feature-backlog.component';
import { CapacityTableComponent } from './capacity-table.component';
import { MilestonesComponent } from './milestones.component';

/** Wie `ProgramIncrement.MAX_TEXT_LENGTH` im Backend; das Formular meldet es nur früher. */
const MAX_TEXT_LENGTH = 10_000;

/**
 * Detailseite eines PI mit Iterationen (US-5), Milestones (US-15) und Business-Kontext samt Links (US-8); die
 * ID kommt aus der Route.
 */
@Component({
  selector: 'app-program-increment-detail',
  imports: [
    DatePipe,
    RouterLink,
    ReactiveFormsModule,
    FeatureBacklogComponent,
    CapacityTableComponent,
    MilestonesComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './program-increment-detail.component.html',
})
export class ProgramIncrementDetailComponent implements OnInit {
  protected readonly service = inject(ProgramIncrementsService);
  private readonly fb = inject(FormBuilder);

  readonly id = input.required<string>();
  protected readonly programIncrement = signal<ProgramIncrement | undefined>(undefined);
  protected readonly hasBusinessContext = computed(() => {
    const pi = this.programIncrement();
    return !!pi && (pi.businessContext !== '' || pi.artObjectives !== '' || pi.links.length > 0);
  });

  /** Feldfehler erst nach dem ersten Absenden zeigen, nicht schon beim Tippen. */
  protected readonly contextSubmitted = signal(false);
  /** Die Formulare öffnen erst auf Knopfdruck, damit der Text nicht doppelt auf der Seite steht (US-8). */
  protected readonly editingContext = signal(false);
  protected readonly addingLink = signal(false);

  protected readonly contextForm = this.fb.nonNullable.group({
    businessContext: ['', Validators.maxLength(MAX_TEXT_LENGTH)],
    artObjectives: ['', Validators.maxLength(MAX_TEXT_LENGTH)],
  });

  protected readonly linkForm = this.fb.nonNullable.group({
    title: ['', notBlank],
    url: ['', notBlank],
  });

  async ngOnInit(): Promise<void> {
    const pi = await this.service.get(this.id());
    if (pi) {
      this.programIncrement.set(pi);
    }
  }

  /** Öffnet das Kontext-Formular mit dem gespeicherten Stand; eine frühere Eingabe ist damit verworfen. */
  protected openContextForm(): void {
    const pi = this.programIncrement();
    this.contextForm.reset({
      businessContext: pi?.businessContext ?? '',
      artObjectives: pi?.artObjectives ?? '',
    });
    this.contextSubmitted.set(false);
    this.editingContext.set(true);
  }

  protected cancelContextForm(): void {
    this.editingContext.set(false);
  }

  protected openLinkForm(): void {
    this.linkForm.reset();
    this.addingLink.set(true);
  }

  protected cancelLinkForm(): void {
    this.addingLink.set(false);
  }

  protected tooLong(field: 'businessContext' | 'artObjectives'): boolean {
    return this.contextSubmitted() && this.contextForm.controls[field].hasError('maxlength');
  }

  protected async saveContext(): Promise<void> {
    this.contextSubmitted.set(true);
    if (this.contextForm.invalid) {
      return;
    }
    const updated = await this.service.updateBusinessContext(
      this.id(),
      this.contextForm.getRawValue(),
    );
    // Bei einem Fehler bleibt das Formular mit der Eingabe offen.
    if (updated) {
      this.programIncrement.set(updated);
      this.editingContext.set(false);
    }
  }

  // Links ändern nur die Anzeige: Ungespeicherte Eingaben im Kontext-Formular bleiben erhalten.
  protected async addLink(): Promise<void> {
    if (this.linkForm.invalid) {
      return;
    }
    const updated = await this.service.addLink(this.id(), this.linkForm.getRawValue());
    if (updated) {
      this.programIncrement.set(updated);
      this.addingLink.set(false);
    }
  }

  protected async removeLink(linkId: string): Promise<void> {
    const updated = await this.service.removeLink(this.id(), linkId);
    if (updated) {
      this.programIncrement.set(updated);
    }
  }
}
