import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Milestone } from '../../core/models/milestone.model';
import { MilestonesRepository } from '../../core/ports/milestones.repository';
import { MilestonesComponent } from './milestones.component';

const PI_ID = 'pi-27-1';
const OUTSIDE_PI = 'Das Datum muss im Zeitraum des PI liegen.';
const EMPTY_STATE = 'Noch keine Milestones.';

// PI 27.1 läuft vom 04.01. bis 26.03.2027 (Delta US-15).
const KICKOFF: Milestone = { id: 'k', name: 'Kickoff', date: '2027-01-11' };
const RELEASE: Milestone = { id: 'r', name: 'Release 4.0', date: '2027-02-10' };

type FieldName = 'Name' | 'Datum';

/**
 * Vertrag mit der Vorlage: je Milestone eine Zeile `[data-testid="milestone-row"]` mit Name, Datum (TT.MM.JJJJ)
 * und den Knöpfen «Bearbeiten» und «Löschen»; das Formular zum Erfassen hat den Knopf «Milestone erfassen», das
 * Formular zum Ändern den Knopf «Speichern». Felder über ihre Beschriftung «Name» und «Datum».
 */
describe('MilestonesComponent', () => {
  let repository: {
    list: ReturnType<typeof vi.fn>;
    add: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    remove: ReturnType<typeof vi.fn>;
  };

  async function render(
    milestones: Milestone[],
    id = PI_ID,
  ): Promise<ComponentFixture<MilestonesComponent>> {
    repository = {
      list: vi.fn().mockResolvedValue(milestones),
      add: vi.fn(),
      update: vi.fn(),
      remove: vi.fn(),
    };
    TestBed.configureTestingModule({
      imports: [MilestonesComponent],
      providers: [{ provide: MilestonesRepository, useValue: repository }],
    });
    const fixture = TestBed.createComponent(MilestonesComponent);
    fixture.componentRef.setInput('programIncrementId', id);
    await fixture.whenStable();
    return fixture;
  }

  function rows(el: HTMLElement): HTMLElement[] {
    return Array.from(el.querySelectorAll<HTMLElement>('[data-testid="milestone-row"]'));
  }

  function text(node: Element): string {
    return (node.textContent ?? '').replace(/\s+/g, ' ').trim();
  }

  function row(el: HTMLElement, name: string): HTMLElement {
    const found = rows(el).find((r) => text(r).includes(name));
    expect(found, `Zeile «${name}»`).toBeDefined();
    return found!;
  }

  function buttonIn(scope: HTMLElement, label: string): HTMLButtonElement | undefined {
    return Array.from(scope.querySelectorAll('button')).find(
      (b) => (b.textContent ?? '').trim() === label,
    );
  }

  function button(scope: HTMLElement, label: string): HTMLButtonElement {
    const found = buttonIn(scope, label);
    expect(found, `Knopf «${label}»`).toBeDefined();
    return found!;
  }

  function formWith(el: HTMLElement, submitLabel: string): HTMLFormElement {
    const form = Array.from(el.querySelectorAll('form')).find((f) => buttonIn(f, submitLabel));
    expect(form, `Formular mit «${submitLabel}»`).toBeDefined();
    return form!;
  }

  function field(form: HTMLElement, name: FieldName): HTMLInputElement {
    const l = Array.from(form.querySelectorAll('label')).find((x) =>
      (x.textContent ?? '').trim().startsWith(name),
    );
    expect(l, `Beschriftung «${name}»`).toBeDefined();
    const control =
      l!.querySelector<HTMLInputElement>('input') ??
      (l!.htmlFor ? form.querySelector<HTMLInputElement>(`#${CSS.escape(l!.htmlFor)}`) : null);
    expect(control, `Feld «${name}»`).not.toBeNull();
    return control!;
  }

  function type(control: HTMLInputElement, value: string): void {
    control.value = value;
    control.dispatchEvent(new Event('input'));
  }

  async function click(
    fixture: ComponentFixture<MilestonesComponent>,
    target: HTMLButtonElement,
  ): Promise<void> {
    target.click();
    await fixture.whenStable();
  }

  async function addMilestone(
    fixture: ComponentFixture<MilestonesComponent>,
    name: string,
    date: string,
  ): Promise<void> {
    const form = formWith(fixture.nativeElement as HTMLElement, 'Milestone erfassen');
    type(field(form, 'Name'), name);
    type(field(form, 'Datum'), date);
    await click(fixture, button(form, 'Milestone erfassen'));
  }

  async function startEdit(
    fixture: ComponentFixture<MilestonesComponent>,
    name: string,
  ): Promise<HTMLFormElement> {
    const el = fixture.nativeElement as HTMLElement;
    await click(fixture, button(row(el, name), 'Bearbeiten'));
    return formWith(el, 'Speichern');
  }

  function alert(el: HTMLElement): string {
    return el.querySelector('[role="alert"]')?.textContent ?? '';
  }

  // --- Anzeige ---

  it('zeigt den Abschnitt «Milestones» und lädt die Milestones des PI (US-15)', async () => {
    const el = (await render([], 'pi-8')).nativeElement as HTMLElement;

    expect(repository.list).toHaveBeenCalledWith('pi-8');
    expect(text(el.querySelector('h2')!)).toBe('Milestones');
  });

  it('listet die Milestones mit Name und Datum als TT.MM.JJJJ in der Reihenfolge des Servers (US-15)', async () => {
    const el = (await render([KICKOFF, RELEASE])).nativeElement as HTMLElement;

    const shown = rows(el).map(text);
    expect(shown).toHaveLength(2);
    expect(shown[0]).toContain('Kickoff');
    expect(shown[0]).toContain('11.01.2027');
    expect(shown[1]).toContain('Release 4.0');
    expect(shown[1]).toContain('10.02.2027');
  });

  it('zeigt ohne Milestones den Leerzustand (US-15)', async () => {
    const el = (await render([])).nativeElement as HTMLElement;

    expect(el.textContent).toContain(EMPTY_STATE);
    expect(rows(el)).toHaveLength(0);
  });

  it('zeigt keinen Leerzustand, sobald es einen Milestone gibt (US-15)', async () => {
    const el = (await render([RELEASE])).nativeElement as HTMLElement;

    expect(el.textContent).not.toContain(EMPTY_STATE);
  });

  // --- AK 1: erfassen ---

  it('bietet zum Erfassen ein Namensfeld und ein Datumsfeld an (US-15)', async () => {
    const form = formWith((await render([])).nativeElement as HTMLElement, 'Milestone erfassen');

    expect(field(form, 'Name')).toBeDefined();
    expect(field(form, 'Datum').type).toBe('date');
  });

  it('erfasst einen Milestone mit Name und Datum und zeigt ihn in der Liste (US-15)', async () => {
    const fixture = await render([KICKOFF]);
    repository.add.mockResolvedValue(RELEASE);

    await addMilestone(fixture, 'Release 4.0', '2027-02-10');

    expect(repository.add).toHaveBeenCalledWith(PI_ID, { name: 'Release 4.0', date: '2027-02-10' });
    const el = fixture.nativeElement as HTMLElement;
    expect(text(row(el, 'Release 4.0'))).toContain('10.02.2027');
    expect(el.textContent).not.toContain(EMPTY_STATE);
    expect(alert(el)).toBe('');
  });

  it('schickt einen Namen aus Leerzeichen nicht ab (US-15)', async () => {
    const fixture = await render([]);

    await addMilestone(fixture, ' ', '2027-02-10');

    expect(repository.add).not.toHaveBeenCalled();
  });

  it('schickt einen Milestone ohne Datum nicht ab (US-15)', async () => {
    const fixture = await render([]);

    await addMilestone(fixture, 'Release 4.0', '');

    expect(repository.add).not.toHaveBeenCalled();
  });

  // --- AK 2: Datum ausserhalb des PI ---

  it('zeigt die Meldung des Servers als Alarm, wenn das Datum ausserhalb des PI liegt (US-15)', async () => {
    const fixture = await render([KICKOFF]);
    repository.add.mockRejectedValue(
      new HttpErrorResponse({ status: 400, error: { message: OUTSIDE_PI } }),
    );

    await addMilestone(fixture, 'Release 4.0', '2027-04-01');

    const el = fixture.nativeElement as HTMLElement;
    expect(repository.add).toHaveBeenCalledWith(PI_ID, { name: 'Release 4.0', date: '2027-04-01' });
    expect(alert(el)).toContain(OUTSIDE_PI);
    expect(rows(el).map(text)).toEqual([expect.stringContaining('Kickoff')]);
  });

  // --- AK 3: ändern ---

  it('bietet je Milestone «Bearbeiten» und «Löschen» an (US-15)', async () => {
    const el = (await render([KICKOFF, RELEASE])).nativeElement as HTMLElement;

    expect(rows(el)).toHaveLength(2);
    for (const r of rows(el)) {
      expect(buttonIn(r, 'Bearbeiten'), text(r)).toBeDefined();
      expect(buttonIn(r, 'Löschen'), text(r)).toBeDefined();
    }
  });

  it('füllt beim Bearbeiten das Formular mit Name und Datum des Milestones (US-15)', async () => {
    const fixture = await render([KICKOFF, RELEASE]);

    const form = await startEdit(fixture, 'Release 4.0');

    expect(field(form, 'Name').value).toBe('Release 4.0');
    expect(field(form, 'Datum').value).toBe('2027-02-10');
  });

  it('verschiebt einen Milestone auf ein neues Datum und zeigt es (US-15)', async () => {
    const fixture = await render([RELEASE]);
    repository.update.mockResolvedValue({ ...RELEASE, date: '2027-01-05' });
    const form = await startEdit(fixture, 'Release 4.0');

    type(field(form, 'Datum'), '2027-01-05');
    await click(fixture, button(form, 'Speichern'));

    expect(repository.update).toHaveBeenCalledWith(PI_ID, 'r', {
      name: 'Release 4.0',
      date: '2027-01-05',
    });
    expect(repository.add).not.toHaveBeenCalled();
    const shown = text(row(fixture.nativeElement as HTMLElement, 'Release 4.0'));
    expect(shown).toContain('05.01.2027');
    expect(shown).not.toContain('10.02.2027');
  });

  it('benennt einen Milestone um und zeigt den neuen Namen (US-15)', async () => {
    const fixture = await render([KICKOFF, RELEASE]);
    repository.update.mockResolvedValue({ ...RELEASE, name: 'Release 4.1' });
    const form = await startEdit(fixture, 'Release 4.0');

    type(field(form, 'Name'), 'Release 4.1');
    await click(fixture, button(form, 'Speichern'));

    expect(repository.update).toHaveBeenCalledWith(PI_ID, 'r', {
      name: 'Release 4.1',
      date: '2027-02-10',
    });
    const el = fixture.nativeElement as HTMLElement;
    expect(rows(el).map(text)).toEqual([
      expect.stringContaining('Kickoff'),
      expect.stringContaining('Release 4.1'),
    ]);
  });

  it('speichert beim Bearbeiten einen Namen aus Leerzeichen nicht (US-15)', async () => {
    const fixture = await render([RELEASE]);
    const form = await startEdit(fixture, 'Release 4.0');

    type(field(form, 'Name'), ' ');
    await click(fixture, button(form, 'Speichern'));

    expect(repository.update).not.toHaveBeenCalled();
  });

  it('zeigt die Meldung des Servers als Alarm, wenn das Ändern abgelehnt wird (US-15)', async () => {
    const fixture = await render([RELEASE]);
    repository.update.mockRejectedValue(
      new HttpErrorResponse({ status: 400, error: { message: OUTSIDE_PI } }),
    );
    const form = await startEdit(fixture, 'Release 4.0');

    type(field(form, 'Datum'), '2027-04-01');
    await click(fixture, button(form, 'Speichern'));

    const el = fixture.nativeElement as HTMLElement;
    expect(alert(el)).toContain(OUTSIDE_PI);
    expect(text(row(el, 'Release 4.0'))).toContain('10.02.2027');
  });

  // --- AK 3: löschen nach Bestätigung ---

  it('löscht einen Milestone erst nach der Bestätigung und zeigt dann den Leerzustand (US-15)', async () => {
    const fixture = await render([RELEASE]);
    repository.remove.mockResolvedValue(undefined);
    const el = fixture.nativeElement as HTMLElement;

    await click(fixture, button(row(el, 'Release 4.0'), 'Löschen'));

    expect(repository.remove).not.toHaveBeenCalled();
    expect(text(row(el, 'Release 4.0'))).toContain('Wirklich löschen?');

    await click(fixture, button(row(el, 'Release 4.0'), 'Wirklich löschen'));

    expect(repository.remove).toHaveBeenCalledWith(PI_ID, 'r');
    expect(rows(el)).toHaveLength(0);
    expect(el.textContent).toContain(EMPTY_STATE);
  });

  it('behält den Milestone, wenn das Löschen abgebrochen wird (US-15)', async () => {
    const fixture = await render([RELEASE]);
    const el = fixture.nativeElement as HTMLElement;
    await click(fixture, button(row(el, 'Release 4.0'), 'Löschen'));

    await click(fixture, button(row(el, 'Release 4.0'), 'Abbrechen'));

    expect(repository.remove).not.toHaveBeenCalled();
    expect(buttonIn(row(el, 'Release 4.0'), 'Wirklich löschen')).toBeUndefined();
    expect(buttonIn(row(el, 'Release 4.0'), 'Löschen')).toBeDefined();
  });

  it('fragt nur beim gewählten Milestone nach der Bestätigung (US-15)', async () => {
    const fixture = await render([KICKOFF, RELEASE]);
    const el = fixture.nativeElement as HTMLElement;

    await click(fixture, button(row(el, 'Release 4.0'), 'Löschen'));

    expect(buttonIn(row(el, 'Release 4.0'), 'Wirklich löschen')).toBeDefined();
    expect(buttonIn(row(el, 'Kickoff'), 'Wirklich löschen')).toBeUndefined();
  });

  it('löscht nur den bestätigten Milestone und behält die anderen (US-15)', async () => {
    const fixture = await render([KICKOFF, RELEASE]);
    repository.remove.mockResolvedValue(undefined);
    const el = fixture.nativeElement as HTMLElement;
    await click(fixture, button(row(el, 'Release 4.0'), 'Löschen'));

    await click(fixture, button(row(el, 'Release 4.0'), 'Wirklich löschen'));

    expect(repository.remove).toHaveBeenCalledWith(PI_ID, 'r');
    expect(rows(el).map(text)).toEqual([expect.stringContaining('Kickoff')]);
  });

  it('zeigt die Meldung des Servers als Alarm und behält den Milestone, wenn das Löschen scheitert (US-15)', async () => {
    const fixture = await render([RELEASE]);
    repository.remove.mockRejectedValue(
      new HttpErrorResponse({ status: 404, error: { message: 'Den Milestone gibt es nicht.' } }),
    );
    const el = fixture.nativeElement as HTMLElement;
    await click(fixture, button(row(el, 'Release 4.0'), 'Löschen'));

    await click(fixture, button(row(el, 'Release 4.0'), 'Wirklich löschen'));

    expect(alert(el)).toContain('Den Milestone gibt es nicht.');
    expect(rows(el).map(text)).toEqual([expect.stringContaining('Release 4.0')]);
  });
});
