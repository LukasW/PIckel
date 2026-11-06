import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProgramIncrementsService } from '../../core/services/program-increments.service';

@Component({
  selector: 'app-program-increments',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './program-increments.component.html',
})
export class ProgramIncrementsComponent implements OnInit {
  protected readonly service = inject(ProgramIncrementsService);
  private readonly fb = inject(FormBuilder);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
  });

  ngOnInit(): void {
    void this.service.load();
  }

  protected async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (await this.service.plan(this.form.getRawValue())) {
      this.form.reset();
    }
  }
}
