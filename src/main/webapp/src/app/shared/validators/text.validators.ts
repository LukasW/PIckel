import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Wie `Validators.required`, lehnt aber auch Text aus Leerzeichen ab (Fehlerschlüssel `required`). */
export function notBlank(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim() ? null : { required: true };
}

/**
 * Wie `Validators.maxLength`, misst aber ohne Leerzeichen am Rand, wie der Server, der den Text vorher trimmt
 * (Fehlerschlüssel `maxlength`).
 */
export function maxTrimmedLength(max: number): ValidatorFn {
  return (control: AbstractControl<string>) =>
    control.value.trim().length > max ? { maxlength: { requiredLength: max } } : null;
}
