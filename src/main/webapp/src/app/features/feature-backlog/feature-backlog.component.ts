import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
} from '@angular/forms';
import { Feature, NewFeature } from '../../core/models/feature.model';
import { FeaturesService } from '../../core/services/features.service';
import { maxTrimmedLength, notBlank } from '../../shared/validators/text.validators';

/** Wie die Grenzen in `Feature` im Backend, gemessen nach dem Trimmen; das Formular meldet sie nur früher. */
const MAX_TITLE_LENGTH = 200;
const MAX_DESCRIPTION_LENGTH = 5000;
const MAX_OWNER_LENGTH = 100;
const MIN_WSJF_RANK = 1;
const MIN_ESTIMATE = 0;

/** Leer oder eine ganze Zahl ab `min` (Fehlerschlüssel `integer`); Text statt Zahlenfeld, damit «1.5» sichtbar bleibt. */
function optionalIntegerFrom(min: number) {
  return (control: AbstractControl<string>): ValidationErrors | null => {
    const value = control.value.trim();
    return value === '' || (/^-?\d+$/.test(value) && Number(value) >= min)
      ? null
      : { integer: true };
  };
}

/**
 * Backlog der Features eines PI mit Formular zum Erfassen (US-9) und Bearbeiten, Löschen nach Bestätigung (US-10);
 * die PI-ID kommt vom Elternelement.
 */
@Component({
  selector: 'app-feature-backlog',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './feature-backlog.component.html',
})
export class FeatureBacklogComponent implements OnInit {
  protected readonly service = inject(FeaturesService);
  private readonly fb = inject(FormBuilder);

  readonly programIncrementId = input.required<string>();

  protected readonly maxTitleLength = MAX_TITLE_LENGTH;
  protected readonly maxDescriptionLength = MAX_DESCRIPTION_LENGTH;
  protected readonly maxOwnerLength = MAX_OWNER_LENGTH;
  /** Feldfehler erst nach dem ersten Absenden zeigen, nicht schon beim Tippen. */
  protected readonly submitted = signal(false);
  /** ID des Features, das gerade im Formular bearbeitet wird; ohne ID erfasst das Formular ein neues. */
  protected readonly editing = signal<string | undefined>(undefined);
  /** ID des Features, dessen Löschen auf die Bestätigung wartet. */
  protected readonly confirmingRemoval = signal<string | undefined>(undefined);

  protected readonly form = this.fb.nonNullable.group({
    title: ['', [notBlank, maxTrimmedLength(MAX_TITLE_LENGTH)]],
    description: ['', maxTrimmedLength(MAX_DESCRIPTION_LENGTH)],
    wsjfRank: ['', optionalIntegerFrom(MIN_WSJF_RANK)],
    owner: ['', maxTrimmedLength(MAX_OWNER_LENGTH)],
    estimate: ['', optionalIntegerFrom(MIN_ESTIMATE)],
  });

  ngOnInit(): void {
    void this.service.load(this.programIncrementId());
  }

  protected hasError(field: keyof typeof this.form.controls, error: string): boolean {
    return this.submitted() && this.form.controls[field].hasError(error);
  }

  protected async submit(): Promise<void> {
    this.submitted.set(true);
    if (this.form.invalid) {
      return;
    }
    const editing = this.editing();
    const saved = editing
      ? await this.service.update(this.programIncrementId(), editing, this.toNewFeature())
      : await this.service.add(this.programIncrementId(), this.toNewFeature());
    if (saved) {
      this.resetForm();
    }
  }

  /** Bearbeiten nutzt das Formular zum Erfassen, damit dieselben Regeln und Feldmeldungen gelten. */
  protected startEdit(feature: Feature): void {
    this.confirmingRemoval.set(undefined);
    this.editing.set(feature.id);
    this.submitted.set(false);
    this.form.setValue({
      title: feature.title,
      description: feature.description,
      wsjfRank: feature.wsjfRank?.toString() ?? '',
      owner: feature.owner,
      estimate: feature.estimate?.toString() ?? '',
    });
  }

  protected cancelEdit(): void {
    this.resetForm();
  }

  protected askRemoval(feature: Feature): void {
    this.confirmingRemoval.set(feature.id);
  }

  protected cancelRemoval(): void {
    this.confirmingRemoval.set(undefined);
  }

  protected async confirmRemoval(feature: Feature): Promise<void> {
    if (await this.service.remove(this.programIncrementId(), feature.id)) {
      this.confirmingRemoval.set(undefined);
      if (this.editing() === feature.id) {
        this.resetForm();
      }
    }
  }

  /** Liest die gewählte CSV-Datei und importiert sie (US-11); die Regeln prüft der Server. */
  protected async importFile(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    const csv = await file.text();
    // Zurücksetzen, damit dieselbe Datei nach einer Korrektur nochmals gewählt werden kann.
    input.value = '';
    await this.service.importCsv(this.programIncrementId(), csv);
  }

  private resetForm(): void {
    this.form.reset();
    this.submitted.set(false);
    this.editing.set(undefined);
  }

  // Leere optionale Angaben fehlen im Objekt, statt als leerer Text oder 0 gesendet zu werden.
  private toNewFeature(): NewFeature {
    const value = this.form.getRawValue();
    const feature: NewFeature = { title: value.title.trim() };
    const description = value.description.trim();
    const wsjfRank = value.wsjfRank.trim();
    const owner = value.owner.trim();
    const estimate = value.estimate.trim();
    if (description) {
      feature.description = description;
    }
    if (wsjfRank) {
      feature.wsjfRank = Number(wsjfRank);
    }
    if (owner) {
      feature.owner = owner;
    }
    if (estimate) {
      feature.estimate = Number(estimate);
    }
    return feature;
  }
}
