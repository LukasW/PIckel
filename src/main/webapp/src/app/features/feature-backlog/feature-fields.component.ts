import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
} from '@angular/forms';
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

export type FeatureForm = FormGroup<{
  title: FormControl<string>;
  description: FormControl<string>;
  wsjfRank: FormControl<string>;
  owner: FormControl<string>;
  estimate: FormControl<string>;
}>;

/** Ein Formular für Erfassen und Bearbeiten, damit beide dieselben Regeln haben (US-10). */
export function buildFeatureForm(fb: FormBuilder): FeatureForm {
  return fb.nonNullable.group({
    title: ['', [notBlank, maxTrimmedLength(MAX_TITLE_LENGTH)]],
    description: ['', maxTrimmedLength(MAX_DESCRIPTION_LENGTH)],
    wsjfRank: ['', optionalIntegerFrom(MIN_WSJF_RANK)],
    owner: ['', maxTrimmedLength(MAX_OWNER_LENGTH)],
    estimate: ['', optionalIntegerFrom(MIN_ESTIMATE)],
  });
}

/** Die Felder eines Features mit ihren Meldungen; das umgebende `<form>` und die Knöpfe stellt das Elternelement. */
@Component({
  selector: 'app-feature-fields',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  // `display: contents`, damit die Felder im Raster des umgebenden Formulars liegen.
  host: { class: 'contents' },
  templateUrl: './feature-fields.component.html',
})
export class FeatureFieldsComponent {
  readonly form = input.required<FeatureForm>();
  /** Feldfehler erst nach dem ersten Absenden zeigen, nicht schon beim Tippen. */
  readonly submitted = input.required<boolean>();

  protected readonly maxTitleLength = MAX_TITLE_LENGTH;
  protected readonly maxDescriptionLength = MAX_DESCRIPTION_LENGTH;
  protected readonly maxOwnerLength = MAX_OWNER_LENGTH;

  protected hasError(field: keyof FeatureForm['controls'], error: string): boolean {
    return this.submitted() && this.form().controls[field].hasError(error);
  }
}
