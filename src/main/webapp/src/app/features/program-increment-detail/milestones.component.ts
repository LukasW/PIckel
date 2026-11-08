import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Milestone } from '../../core/models/milestone.model';
import { MilestonesService } from '../../core/services/milestones.service';
import { notBlank } from '../../shared/validators/text.validators';

/** Wie `Milestone.MAX_NAME_LENGTH` im Backend; das Feld begrenzt die Eingabe nur früher. */
const MAX_NAME_LENGTH = 100;

/**
 * Milestones eines PI auf der Detailseite (US-15): Liste, Formular zum Erfassen und Ändern, Löschen nach
 * Bestätigung.
 */
@Component({
  selector: 'app-milestones',
  imports: [DatePipe, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './milestones.component.html',
})
export class MilestonesComponent {
  protected readonly service = inject(MilestonesService);
  private readonly fb = inject(FormBuilder);

  readonly programIncrementId = input.required<string>();

  protected readonly maxNameLength = MAX_NAME_LENGTH;

  /** ID des Milestones, der gerade bearbeitet wird. */
  protected readonly editing = signal<string | undefined>(undefined);
  /** ID des Milestones, dessen Löschen auf die Bestätigung wartet. */
  protected readonly confirmingRemoval = signal<string | undefined>(undefined);

  protected readonly addForm = this.fb.nonNullable.group({
    name: ['', notBlank],
    date: ['', Validators.required],
  });

  protected readonly editForm = this.fb.nonNullable.group({
    name: ['', notBlank],
    date: ['', Validators.required],
  });

  constructor() {
    effect(() => void this.service.load(this.programIncrementId()));
  }

  protected async add(): Promise<void> {
    if (this.addForm.invalid) {
      return;
    }
    // Getrimmt wird nur auf dem Server, damit die Regel an einem Ort steht.
    if (await this.service.add(this.programIncrementId(), this.addForm.getRawValue())) {
      this.addForm.reset();
    }
  }

  protected startEdit(milestone: Milestone): void {
    this.confirmingRemoval.set(undefined);
    this.editing.set(milestone.id);
    this.editForm.reset({ name: milestone.name, date: milestone.date });
  }

  protected cancelEdit(): void {
    this.editing.set(undefined);
  }

  protected async save(): Promise<void> {
    const id = this.editing();
    if (!id || this.editForm.invalid) {
      return;
    }
    if (await this.service.update(this.programIncrementId(), id, this.editForm.getRawValue())) {
      this.editing.set(undefined);
    }
  }

  protected askRemoval(milestone: Milestone): void {
    this.editing.set(undefined);
    this.confirmingRemoval.set(milestone.id);
  }

  protected cancelRemoval(): void {
    this.confirmingRemoval.set(undefined);
  }

  protected async confirmRemoval(milestone: Milestone): Promise<void> {
    // Scheitert das Löschen, bleibt die Rückfrage stehen: Die Meldung steht oben, ein zweiter Versuch geht direkt.
    if (await this.service.remove(this.programIncrementId(), milestone.id)) {
      this.confirmingRemoval.set(undefined);
    }
  }
}
