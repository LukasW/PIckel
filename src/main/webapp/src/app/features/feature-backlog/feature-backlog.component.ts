import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Feature, NewFeature } from '../../core/models/feature.model';
import { FeaturesService } from '../../core/services/features.service';
import { FeatureDependenciesService } from '../../core/services/feature-dependencies.service';
import { buildFeatureForm, FeatureFieldsComponent, FeatureForm } from './feature-fields.component';

/** Kopfzeile wie in `FeatureCsv` im Backend, dazu zwei Zeilen als Muster (US-11). */
const SAMPLE_CSV =
  'Titel;Beschreibung;WSJF-Rang;Owner;Schätzung\n' +
  'Login;Anmeldung mit SSO;1;Anna Muster;8\n' +
  'Suche;;2;;5\n';

/** Meldung einer gescheiterten Aktion an Abhängigkeiten, bei der Zeile des Features, das sie auslöste (US-16). */
interface RowError {
  readonly featureId: string;
  readonly message: string;
}

/**
 * Backlog der Features eines PI mit Formular zum Erfassen (US-9) und CSV-Import (US-11), Bearbeiten in der Zeile
 * und Löschen nach Bestätigung (US-10) und Abhängigkeiten in beide Richtungen (US-16); die PI-ID kommt vom
 * Elternelement.
 */
@Component({
  selector: 'app-feature-backlog',
  imports: [ReactiveFormsModule, FeatureFieldsComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './feature-backlog.component.html',
})
export class FeatureBacklogComponent implements OnInit {
  protected readonly service = inject(FeaturesService);
  protected readonly dependencies = inject(FeatureDependenciesService);
  private readonly fb = inject(FormBuilder);

  readonly programIncrementId = input.required<string>();

  protected readonly sampleCsvHref =
    'data:text/csv;charset=utf-8,' + encodeURIComponent(SAMPLE_CSV);
  /** Feldfehler erst nach dem ersten Absenden zeigen, nicht schon beim Tippen. */
  protected readonly submitted = signal(false);
  protected readonly editSubmitted = signal(false);
  /** ID des Features, dessen Zeile gerade das Formular zum Bearbeiten zeigt; höchstens eines zugleich. */
  protected readonly editing = signal<string | undefined>(undefined);
  /** Gewähltes Feature in der Auswahl «Braucht Feature», je Zeile. */
  protected readonly chosenNeeded = signal<Readonly<Record<string, string>>>({});
  protected readonly dependencyError = signal<RowError | undefined>(undefined);
  /** ID des Features, dessen Löschen auf die Bestätigung wartet. */
  protected readonly confirmingRemoval = signal<string | undefined>(undefined);

  protected readonly form = buildFeatureForm(this.fb);
  protected readonly editForm = buildFeatureForm(this.fb);

  ngOnInit(): void {
    void this.service.load(this.programIncrementId());
    void this.dependencies.load(this.programIncrementId());
  }

  /** Die Features zu den IDs in Backlog-Reihenfolge; IDs ausserhalb des Backlogs fallen weg. */
  protected featuresWithIds(ids: string[]): Feature[] {
    return this.service.features().filter((feature) => ids.includes(feature.id));
  }

  /**
   * Die anderen Features des Backlogs, mit denen das Feature in keiner Richtung verbunden ist (US-16): Eine doppelte
   * oder umgekehrte Abhängigkeit lehnt der Server ohnehin ab. Die übrigen Regeln prüft der Server.
   */
  protected candidatesFor(feature: Feature): Feature[] {
    const connected = new Set([
      feature.id,
      ...this.dependencies.needs(feature.id),
      ...this.dependencies.neededBy(feature.id),
    ]);
    return this.service.features().filter((other) => !connected.has(other.id));
  }

  protected chooseNeeded(feature: Feature, neededId: string): void {
    this.chosenNeeded.update((chosen) => ({ ...chosen, [feature.id]: neededId }));
  }

  /** Die Wahl gilt nur, solange das Feature noch zur Auswahl steht; sonst zeigt die Auswahl den Platzhalter. */
  protected neededChoice(feature: Feature): string | undefined {
    const neededId = this.chosenNeeded()[feature.id];
    return this.candidatesFor(feature).some((other) => other.id === neededId)
      ? neededId
      : undefined;
  }

  protected async addDependency(feature: Feature, select: HTMLSelectElement): Promise<void> {
    const neededId = this.neededChoice(feature);
    if (!neededId) {
      return;
    }
    if (
      await this.changeDependency(feature, () =>
        this.dependencies.add(this.programIncrementId(), feature.id, neededId),
      )
    ) {
      select.value = '';
      this.chooseNeeded(feature, '');
    }
  }

  protected removeDependency(feature: Feature, needed: Feature): Promise<boolean> {
    return this.changeDependency(feature, () =>
      this.dependencies.remove(this.programIncrementId(), feature.id, needed.id),
    );
  }

  protected async submit(): Promise<void> {
    this.submitted.set(true);
    if (this.form.invalid) {
      return;
    }
    if (await this.service.add(this.programIncrementId(), toNewFeature(this.form))) {
      this.form.reset();
      this.submitted.set(false);
    }
  }

  protected async saveEdit(): Promise<void> {
    this.editSubmitted.set(true);
    const editing = this.editing();
    if (!editing || this.editForm.invalid) {
      return;
    }
    if (
      await this.service.update(this.programIncrementId(), editing, toNewFeature(this.editForm))
    ) {
      this.editing.set(undefined);
    }
  }

  /** Bearbeitet wird in der Zeile des Features (US-10); ein anderes offenes Bearbeiten wird verworfen. */
  protected startEdit(feature: Feature): void {
    this.confirmingRemoval.set(undefined);
    this.editing.set(feature.id);
    this.editSubmitted.set(false);
    this.editForm.setValue({
      title: feature.title,
      description: feature.description,
      wsjfRank: feature.wsjfRank?.toString() ?? '',
      owner: feature.owner,
      estimate: feature.estimate?.toString() ?? '',
    });
  }

  protected cancelEdit(): void {
    this.editing.set(undefined);
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
      // Der Server löscht die Abhängigkeiten des Features mit (US-16).
      await this.dependencies.load(this.programIncrementId());
      if (this.editing() === feature.id) {
        this.editing.set(undefined);
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

  /** Eine Meldung gehört zur Zeile, deren Aktion scheiterte, und gilt nur bis zur nächsten Aktion (US-16). */
  private async changeDependency(
    feature: Feature,
    change: () => Promise<boolean>,
  ): Promise<boolean> {
    this.dependencyError.set(undefined);
    const changed = await change();
    if (!changed) {
      this.dependencyError.set({ featureId: feature.id, message: this.dependencies.error() ?? '' });
    }
    return changed;
  }
}

// Leere optionale Angaben fehlen im Objekt, statt als leerer Text oder 0 gesendet zu werden.
function toNewFeature(form: FeatureForm): NewFeature {
  const value = form.getRawValue();
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
