import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
} from '@angular/forms';
import { NewFeature } from '../../core/models/feature.model';
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

/** Backlog und Erfassungsformular der Features eines PI (US-9); die PI-ID kommt vom Elternelement. */
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
    if (await this.service.add(this.programIncrementId(), this.toNewFeature())) {
      this.form.reset();
      this.submitted.set(false);
    }
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
