import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Team } from '../../core/models/team.model';
import { TeamsService } from '../../core/services/teams.service';
import { maxTrimmedLength, notBlank } from '../../shared/validators/text.validators';

/** Wie `Team.MAX_NAME_LENGTH` im Backend, gemessen nach dem Trimmen; das Formular meldet es nur früher. */
const MAX_NAME_LENGTH = 50;

@Component({
  selector: 'app-teams',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './teams.component.html',
})
export class TeamsComponent implements OnInit {
  protected readonly service = inject(TeamsService);
  private readonly fb = inject(FormBuilder);

  protected readonly maxNameLength = MAX_NAME_LENGTH;
  /** Feldfehler erst nach dem ersten Absenden zeigen, nicht schon beim Tippen. */
  protected readonly submitted = signal(false);
  /** ID des Teams, dessen Entfernen auf die Bestätigung wartet. */
  protected readonly confirmingRemoval = signal<string | undefined>(undefined);
  /** ID des Teams, das gerade umbenannt wird. */
  protected readonly editing = signal<string | undefined>(undefined);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [notBlank, maxTrimmedLength(MAX_NAME_LENGTH)]],
  });
  protected readonly renameControl = this.fb.nonNullable.control('', [
    notBlank,
    maxTrimmedLength(MAX_NAME_LENGTH),
  ]);

  ngOnInit(): void {
    void this.service.load();
  }

  protected hasError(error: string): boolean {
    return this.submitted() && this.form.controls.name.hasError(error);
  }

  protected async submit(): Promise<void> {
    this.submitted.set(true);
    if (this.form.invalid) {
      return;
    }
    if (await this.service.add(this.form.getRawValue().name.trim())) {
      this.form.reset();
      this.submitted.set(false);
    }
  }

  protected startRename(team: Team): void {
    this.confirmingRemoval.set(undefined);
    this.renameControl.reset(team.name);
    this.editing.set(team.id);
  }

  protected cancelRename(): void {
    this.editing.set(undefined);
  }

  protected async saveRename(team: Team): Promise<void> {
    if (this.renameControl.invalid) {
      this.renameControl.markAsTouched();
      return;
    }
    if (await this.service.rename(team.id, this.renameControl.value.trim())) {
      this.editing.set(undefined);
    }
  }

  protected askRemoval(team: Team): void {
    this.editing.set(undefined);
    this.confirmingRemoval.set(team.id);
  }

  protected cancelRemoval(): void {
    this.confirmingRemoval.set(undefined);
  }

  protected async confirmRemoval(team: Team): Promise<void> {
    this.confirmingRemoval.set(undefined);
    await this.service.remove(team.id);
  }
}
