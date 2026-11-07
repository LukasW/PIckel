import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProgramIncrementsService } from '../../core/services/program-increments.service';
import { maxTrimmedLength, notBlank } from '../../shared/validators/text.validators';

/** Wie `ProgramIncrement.MAX_NAME_LENGTH` im Backend, gemessen nach dem Trimmen; das Formular meldet es nur früher. */
const MAX_NAME_LENGTH = 100;

/** Wie im Backend: erlaubt 1 bis 4 Wochen, Standard 2 (US-5). */
const ITERATION_LENGTHS_WEEKS = [1, 2, 3, 4] as const;
const DEFAULT_ITERATION_LENGTH_WEEKS = 2;

@Component({
  selector: 'app-program-increments',
  imports: [ReactiveFormsModule, DatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './program-increments.component.html',
})
export class ProgramIncrementsComponent implements OnInit {
  protected readonly service = inject(ProgramIncrementsService);
  private readonly fb = inject(FormBuilder);

  protected readonly maxNameLength = MAX_NAME_LENGTH;
  protected readonly iterationLengths = ITERATION_LENGTHS_WEEKS;
  /** Feldfehler erst nach dem ersten Absenden zeigen, nicht schon beim Tippen. */
  protected readonly submitted = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [notBlank, maxTrimmedLength(MAX_NAME_LENGTH)]],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    iterationLengthWeeks: [DEFAULT_ITERATION_LENGTH_WEEKS],
  });

  ngOnInit(): void {
    void this.service.load();
  }

  protected hasError(field: 'name' | 'startDate' | 'endDate', error: string): boolean {
    return this.submitted() && this.form.controls[field].hasError(error);
  }

  protected async submit(): Promise<void> {
    this.submitted.set(true);
    if (this.form.invalid) {
      return;
    }
    if (await this.service.plan(this.form.getRawValue())) {
      this.form.reset();
      this.submitted.set(false);
    }
  }
}
