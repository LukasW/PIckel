import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Feature } from '../../core/models/feature.model';
import { FeatureDependency } from '../../core/models/feature-dependency.model';
import { FeatureDependenciesRepository } from '../../core/ports/feature-dependencies.repository';
import { FeaturesRepository } from '../../core/ports/features.repository';
import { FeatureBacklogComponent } from './feature-backlog.component';

const PI_ID = 'pi-27-1';

function feature(id: string, title: string, extra: Partial<Feature> = {}): Feature {
  return {
    id,
    programIncrementId: PI_ID,
    title,
    description: '',
    owner: '',
    scheduled: false,
    ...extra,
  };
}

const PORTAL = feature('p', 'Self-Service-Portal', { wsjfRank: 1, owner: 'Anna', estimate: 21 });
const LOGIN = feature('l', 'Login');

type FieldName = 'Titel' | 'Beschreibung' | 'WSJF-Rang' | 'Owner' | 'Schätzung';
const FIELDS: FieldName[] = ['Titel', 'Beschreibung', 'WSJF-Rang', 'Owner', 'Schätzung'];

const MESSAGES = {
  titleEmpty: 'Der Titel darf nicht leer sein.',
  titleTooLong: 'Der Titel darf höchstens 200 Zeichen lang sein.',
  descriptionTooLong: 'Die Beschreibung darf höchstens 5000 Zeichen lang sein.',
  ownerTooLong: 'Der Owner darf höchstens 100 Zeichen lang sein.',
  rank: 'Der WSJF-Rang muss eine ganze Zahl ab 1 sein.',
  estimate: 'Die Schätzung muss eine ganze Zahl ab 0 sein.',
};

describe('FeatureBacklogComponent', () => {
  let repository: {
    list: ReturnType<typeof vi.fn>;
    add: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    remove: ReturnType<typeof vi.fn>;
    importCsv: ReturnType<typeof vi.fn>;
  };
  let dependencyRepository: {
    list: ReturnType<typeof vi.fn>;
    add: ReturnType<typeof vi.fn>;
    remove: ReturnType<typeof vi.fn>;
  };
  /** Antworten des Servers auf das erste, zweite, … Laden der Abhängigkeiten (US-16); ohne Angabe keine. */
  let dependencyLists: FeatureDependency[][];

  beforeEach(() => {
    dependencyLists = [];
  });

  /** Rendert das Backlog des PI; `lists` sind die Antworten des Servers auf das erste, zweite, … Laden. */
  async function render(...lists: Feature[][]): Promise<ComponentFixture<FeatureBacklogComponent>> {
    const list = vi.fn();
    for (const answer of lists) {
      list.mockResolvedValueOnce(answer);
    }
    list.mockResolvedValue(lists.at(-1) ?? []);
    repository = { list, add: vi.fn(), update: vi.fn(), remove: vi.fn(), importCsv: vi.fn() };
    const dependencyList = vi.fn();
    for (const answer of dependencyLists) {
      dependencyList.mockResolvedValueOnce(answer);
    }
    dependencyList.mockResolvedValue(dependencyLists.at(-1) ?? []);
    dependencyRepository = { list: dependencyList, add: vi.fn(), remove: vi.fn() };
    TestBed.configureTestingModule({
      imports: [FeatureBacklogComponent],
      providers: [
        { provide: FeaturesRepository, useValue: repository },
        { provide: FeatureDependenciesRepository, useValue: dependencyRepository },
      ],
    });
    const fixture = TestBed.createComponent(FeatureBacklogComponent);
    fixture.componentRef.setInput('programIncrementId', PI_ID);
    await fixture.whenStable();
    return fixture;
  }

  function rows(el: HTMLElement): HTMLElement[] {
    return Array.from(el.querySelectorAll<HTMLElement>('[data-testid="feature-row"]'));
  }

  function hasButton(scope: HTMLElement, label: string): boolean {
    return Array.from(scope.querySelectorAll('button')).some(
      (b) => b.textContent!.trim() === label,
    );
  }

  /** Das Formular zum Erfassen, erkennbar an seinem Button «Feature erfassen». */
  function addForm(el: HTMLElement): HTMLFormElement {
    const form = Array.from(el.querySelectorAll('form')).find((f) =>
      hasButton(f, 'Feature erfassen'),
    );
    expect(form, 'Formular «Feature erfassen»').toBeDefined();
    return form!;
  }

  function label(form: HTMLElement, name: FieldName): HTMLLabelElement {
    const found = Array.from(form.querySelectorAll('label')).find((l) =>
      l.textContent!.trim().startsWith(name),
    );
    expect(found, `Beschriftung «${name}»`).toBeDefined();
    return found!;
  }

  /** Das Eingabefeld zur Beschriftung, verschachtelt oder über `for` verknüpft. */
  function field(form: HTMLElement, name: FieldName): HTMLInputElement | HTMLTextAreaElement {
    const l = label(form, name);
    const control =
      l.querySelector<HTMLInputElement | HTMLTextAreaElement>('input, textarea') ??
      (l.htmlFor
        ? form.querySelector<HTMLInputElement | HTMLTextAreaElement>(`#${CSS.escape(l.htmlFor)}`)
        : null);
    expect(control, `Feld «${name}»`).not.toBeNull();
    return control!;
  }

  /** Der Bereich eines Felds: die Beschriftung, wenn sie das Feld umschliesst, sonst ihr Elternelement. */
  function fieldArea(form: HTMLElement, name: FieldName): HTMLElement {
    const l = label(form, name);
    return l.querySelector('input, textarea') ? l : l.parentElement!;
  }

  function type(control: HTMLInputElement | HTMLTextAreaElement, value: string) {
    control.value = value;
    control.dispatchEvent(new Event('input'));
  }

  async function submit(fixture: ComponentFixture<FeatureBacklogComponent>) {
    const form = addForm(fixture.nativeElement as HTMLElement);
    const button = Array.from(form.querySelectorAll('button')).find(
      (b) => b.textContent!.trim() === 'Feature erfassen',
    )!;
    button.click();
    await fixture.whenStable();
  }

  async function fillAndSubmit(
    fixture: ComponentFixture<FeatureBacklogComponent>,
    values: Partial<Record<FieldName, string>>,
  ): Promise<HTMLFormElement> {
    const el = fixture.nativeElement as HTMLElement;
    for (const [name, value] of Object.entries(values) as [FieldName, string][]) {
      type(field(addForm(el), name), value);
    }
    await submit(fixture);
    return addForm(el);
  }

  /** Prüft, dass die Meldung am Feld `name` steht und an keinem anderen Feld. */
  function expectErrorAt(form: HTMLElement, name: FieldName, message: string) {
    expect(fieldArea(form, name).textContent).toContain(message);
    for (const other of FIELDS.filter((f) => f !== name)) {
      expect(fieldArea(form, other).textContent, `Feld «${other}»`).not.toContain(message);
    }
  }

  it('lädt das Backlog des PI, in dem es eingebettet ist (US-9)', async () => {
    await render([]);

    expect(repository.list).toHaveBeenCalledWith(PI_ID);
  });

  // Den genauen Text legt US-10 fest (Hinweis auf den CSV-Import), siehe den Test dort.
  it('zeigt den Leerzustand ohne Features (US-9)', async () => {
    const el = (await render([])).nativeElement as HTMLElement;

    expect(el.textContent).toContain('Noch keine Features.');
    expect(rows(el)).toHaveLength(0);
  });

  it('zeigt keinen Leerzustand, sobald es ein Feature gibt (US-9)', async () => {
    const el = (await render([LOGIN])).nativeElement as HTMLElement;

    expect(el.textContent).not.toContain('Noch keine Features');
  });

  it('listet die Features in der Reihenfolge des Servers (US-9)', async () => {
    const el = (await render([LOGIN, PORTAL])).nativeElement as HTMLElement;

    expect(rows(el).map(ownText)).toEqual([
      expect.stringContaining('Login'),
      expect.stringContaining('Self-Service-Portal'),
    ]);
  });

  it('erfasst ein Feature mit Rang, Owner und Schätzung, zeigt es im Backlog und leert das Formular (US-9)', async () => {
    const fixture = await render([], [PORTAL]);
    repository.add.mockResolvedValue(PORTAL);

    const form = await fillAndSubmit(fixture, {
      Titel: 'Self-Service-Portal',
      Beschreibung: 'Kundinnen ändern ihre Adresse selbst',
      'WSJF-Rang': '1',
      Owner: 'Anna',
      Schätzung: '21',
    });

    expect(repository.add).toHaveBeenCalledWith(PI_ID, {
      title: 'Self-Service-Portal',
      description: 'Kundinnen ändern ihre Adresse selbst',
      wsjfRank: 1,
      owner: 'Anna',
      estimate: 21,
    });
    const el = fixture.nativeElement as HTMLElement;
    expect(rows(el).map(ownText)).toEqual([expect.stringContaining('Self-Service-Portal')]);
    for (const name of FIELDS) {
      expect(field(form, name).value, `Feld «${name}»`).toBe('');
    }
  });

  it('sendet bei einem Feature nur mit Titel keine leeren optionalen Angaben (US-9)', async () => {
    const fixture = await render([], [LOGIN]);
    repository.add.mockResolvedValue(LOGIN);

    await fillAndSubmit(fixture, { Titel: 'Login' });

    expect(repository.add).toHaveBeenCalledTimes(1);
    const [piId, sent] = repository.add.mock.calls[0];
    expect(piId).toBe(PI_ID);
    expect(sent).toEqual({ title: 'Login' });
    expect(Object.keys(sent)).toEqual(['title']);
  });

  it('sendet den Titel ohne Leerzeichen am Rand (US-9)', async () => {
    const fixture = await render([]);
    repository.add.mockResolvedValue(LOGIN);

    await fillAndSubmit(fixture, { Titel: '  Login  ' });

    expect(repository.add).toHaveBeenCalledWith(PI_ID, { title: 'Login' });
  });

  it('zeigt vor dem ersten Absenden keine Feldfehler (US-9)', async () => {
    const fixture = await render([]);
    const el = fixture.nativeElement as HTMLElement;

    type(field(addForm(el), 'WSJF-Rang'), '0');
    type(field(addForm(el), 'Schätzung'), '-1');
    await fixture.whenStable();

    const text = addForm(el).textContent;
    expect(text).not.toContain(MESSAGES.titleEmpty);
    expect(text).not.toContain(MESSAGES.rank);
    expect(text).not.toContain(MESSAGES.estimate);
  });

  it('lehnt einen Titel aus Leerzeichen am Feld ab und sendet nichts (US-9)', async () => {
    const form = await fillAndSubmit(await render([]), {
      Titel: '   ',
      'WSJF-Rang': '1',
      Owner: 'Anna',
      Schätzung: '21',
    });

    expectErrorAt(form, 'Titel', MESSAGES.titleEmpty);
    expect(repository.add).not.toHaveBeenCalled();
  });

  it('lehnt einen Titel mit 201 Zeichen am Feld ab und sendet nichts (US-9)', async () => {
    const form = await fillAndSubmit(await render([]), { Titel: 'x'.repeat(201) });

    expectErrorAt(form, 'Titel', MESSAGES.titleTooLong);
    expect(repository.add).not.toHaveBeenCalled();
  });

  it('nimmt einen Titel mit genau 200 Zeichen an (US-9)', async () => {
    const fixture = await render([]);
    const title = 'x'.repeat(200);
    repository.add.mockResolvedValue(feature('x', title));

    await fillAndSubmit(fixture, { Titel: title });

    expect(repository.add).toHaveBeenCalledWith(PI_ID, { title });
  });

  it('misst die Länge des Titels ohne Leerzeichen am Rand (US-9)', async () => {
    const fixture = await render([]);
    const title = 'x'.repeat(200);
    repository.add.mockResolvedValue(feature('x', title));

    const form = await fillAndSubmit(fixture, { Titel: '  ' + title + '  ' });

    expect(form.textContent).not.toContain(MESSAGES.titleTooLong);
    expect(repository.add).toHaveBeenCalledWith(PI_ID, { title });
  });

  it('lehnt eine Beschreibung mit 5001 Zeichen am Feld ab und sendet nichts (US-9)', async () => {
    const form = await fillAndSubmit(await render([]), {
      Titel: 'Login',
      Beschreibung: 'x'.repeat(5001),
    });

    expectErrorAt(form, 'Beschreibung', MESSAGES.descriptionTooLong);
    expect(repository.add).not.toHaveBeenCalled();
  });

  it('nimmt eine Beschreibung mit genau 5000 Zeichen an (US-9)', async () => {
    const fixture = await render([]);
    repository.add.mockResolvedValue(LOGIN);

    await fillAndSubmit(fixture, { Titel: 'Login', Beschreibung: 'x'.repeat(5000) });

    expect(repository.add).toHaveBeenCalledWith(PI_ID, {
      title: 'Login',
      description: 'x'.repeat(5000),
    });
  });

  it('lehnt einen Owner mit 101 Zeichen am Feld ab und sendet nichts (US-9)', async () => {
    const form = await fillAndSubmit(await render([]), {
      Titel: 'Login',
      Owner: 'x'.repeat(101),
    });

    expectErrorAt(form, 'Owner', MESSAGES.ownerTooLong);
    expect(repository.add).not.toHaveBeenCalled();
  });

  it('nimmt einen Owner mit genau 100 Zeichen an (US-9)', async () => {
    const fixture = await render([]);
    repository.add.mockResolvedValue(LOGIN);

    await fillAndSubmit(fixture, { Titel: 'Login', Owner: 'x'.repeat(100) });

    expect(repository.add).toHaveBeenCalledWith(PI_ID, { title: 'Login', owner: 'x'.repeat(100) });
  });

  it('lehnt den WSJF-Rang 0 am Feld ab und sendet nichts (US-9)', async () => {
    const form = await fillAndSubmit(await render([]), { Titel: 'Login', 'WSJF-Rang': '0' });

    expectErrorAt(form, 'WSJF-Rang', MESSAGES.rank);
    expect(repository.add).not.toHaveBeenCalled();
  });

  it('lehnt einen WSJF-Rang mit Nachkommastellen am Feld ab und sendet nichts (US-9)', async () => {
    const form = await fillAndSubmit(await render([]), { Titel: 'Login', 'WSJF-Rang': '1.5' });

    expectErrorAt(form, 'WSJF-Rang', MESSAGES.rank);
    expect(repository.add).not.toHaveBeenCalled();
  });

  it('nimmt den WSJF-Rang 1 an (US-9)', async () => {
    const fixture = await render([]);
    repository.add.mockResolvedValue(LOGIN);

    await fillAndSubmit(fixture, { Titel: 'Login', 'WSJF-Rang': '1' });

    expect(repository.add).toHaveBeenCalledWith(PI_ID, { title: 'Login', wsjfRank: 1 });
  });

  it('lehnt die Schätzung -1 am Feld ab und sendet nichts (US-9)', async () => {
    const form = await fillAndSubmit(await render([]), { Titel: 'Login', Schätzung: '-1' });

    expectErrorAt(form, 'Schätzung', MESSAGES.estimate);
    expect(repository.add).not.toHaveBeenCalled();
  });

  it('lehnt eine Schätzung mit Nachkommastellen am Feld ab und sendet nichts (US-9)', async () => {
    const form = await fillAndSubmit(await render([]), { Titel: 'Login', Schätzung: '2.5' });

    expectErrorAt(form, 'Schätzung', MESSAGES.estimate);
    expect(repository.add).not.toHaveBeenCalled();
  });

  it('nimmt die Schätzung 0 an und sendet sie mit (US-9)', async () => {
    const fixture = await render([]);
    repository.add.mockResolvedValue(LOGIN);

    await fillAndSubmit(fixture, { Titel: 'Login', Schätzung: '0' });

    expect(repository.add).toHaveBeenCalledWith(PI_ID, { title: 'Login', estimate: 0 });
  });

  it('zeigt die Meldung des Servers als Alarm und behält die Angaben, wenn das Erfassen abgelehnt wird (US-9)', async () => {
    const fixture = await render([]);
    repository.add.mockRejectedValue(
      new HttpErrorResponse({ status: 404, error: { message: 'Das PI gibt es nicht.' } }),
    );

    const form = await fillAndSubmit(fixture, { Titel: 'Login', Owner: 'Anna' });

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Das PI gibt es nicht.');
    expect(field(form, 'Titel').value).toBe('Login');
    expect(field(form, 'Owner').value).toBe('Anna');
  });

  // --- US-10: Backlog bearbeiten ---

  const EMPTY_STATE =
    'Noch keine Features. Erfasse das erste Feature oder importiere eine CSV-Datei.';
  const EXPORT = feature('e', 'CSV-Export', {
    description: 'Für die Buchhaltung',
    wsjfRank: 2,
    owner: 'Beat',
    estimate: 5,
  });

  /**
   * Text der Zeile ohne die Angaben zu anderen Features: Seit US-16 nennt jede Zeile die Titel anderer Features
   * (Auswahl «Braucht Feature», «Braucht: …», «Wird gebraucht von: …»).
   */
  function ownText(r: HTMLElement): string {
    const copy = r.cloneNode(true) as HTMLElement;
    copy
      .querySelectorAll('select, [data-testid="needs"], [data-testid="needed-by"]')
      .forEach((e) => e.remove());
    return copy.textContent!;
  }

  function row(el: HTMLElement, title: string): HTMLElement {
    const found = rows(el).find((r) => ownText(r).includes(title));
    expect(found, `Zeile von ${title}`).toBeDefined();
    return found!;
  }

  function button(scope: HTMLElement, text: string): HTMLButtonElement {
    const found = Array.from(scope.querySelectorAll('button')).find(
      (b) => b.textContent!.trim() === text,
    );
    expect(found, `Button «${text}»`).toBeDefined();
    return found!;
  }

  /** Prüft, dass die Zeile ein eigenes Element mit der Zahl zeigt (z. B. «#2» oder «5 SP»), ohne andere Ziffern. */
  function expectNumberShown(r: HTMLElement, value: number) {
    const leaves = Array.from(r.querySelectorAll('*'))
      .filter((e) => e.children.length === 0)
      .map((e) => e.textContent!.trim());
    expect(
      leaves.some((t) => new RegExp(`^\\D*${value}\\D*$`).test(t)),
      `Zahl ${value} in ${JSON.stringify(leaves)}`,
    ).toBe(true);
  }

  async function click(fixture: ComponentFixture<FeatureBacklogComponent>, target: HTMLElement) {
    target.click();
    await fixture.whenStable();
  }

  const EDIT_FORM = 'form[aria-label="Feature bearbeiten"]';

  /**
   * Das Formular zum Bearbeiten: Es steht in der Zeile des Features (Delta #50), und es gibt höchstens eines.
   * Das Formular zum Erfassen oben bleibt davon getrennt.
   */
  function editForm(el: HTMLElement): HTMLFormElement {
    const forms = Array.from(el.querySelectorAll<HTMLFormElement>(EDIT_FORM));
    expect(forms, 'genau ein Formular «Feature bearbeiten»').toHaveLength(1);
    expect(
      forms[0].closest('[data-testid="feature-row"]'),
      'Formular «Feature bearbeiten» in einer Zeile des Backlogs',
    ).not.toBeNull();
    return forms[0];
  }

  /** Die Zeile, in der das Formular zum Bearbeiten steht, als Index in der Liste. */
  function editedRowIndex(el: HTMLElement): number {
    const r = editForm(el).closest('[data-testid="feature-row"]');
    return rows(el).indexOf(r as HTMLElement);
  }

  async function saveEdit(fixture: ComponentFixture<FeatureBacklogComponent>) {
    await click(fixture, button(editForm(fixture.nativeElement as HTMLElement), 'Speichern'));
  }

  /** Wählt «Bearbeiten» in der Zeile des Features und prüft, dass genau diese Zeile zum Formular wird. */
  async function startEdit(
    fixture: ComponentFixture<FeatureBacklogComponent>,
    title: string,
  ): Promise<HTMLFormElement> {
    const el = fixture.nativeElement as HTMLElement;
    const index = rows(el).indexOf(row(el, title));
    await click(fixture, button(row(el, title), 'Bearbeiten'));
    expect(editedRowIndex(el), `Formular in der Zeile von ${title}`).toBe(index);
    return editForm(el);
  }

  async function editAndSubmit(
    fixture: ComponentFixture<FeatureBacklogComponent>,
    title: string,
    values: Partial<Record<FieldName, string>>,
  ): Promise<void> {
    const el = fixture.nativeElement as HTMLElement;
    await startEdit(fixture, title);
    for (const [name, value] of Object.entries(values) as [FieldName, string][]) {
      type(field(editForm(el), name), value);
    }
    await saveEdit(fixture);
  }

  /** Prüft, dass das Formular zum Erfassen leer ist und keine Meldung zeigt. */
  function expectAddFormUntouched(el: HTMLElement) {
    const form = addForm(el);
    for (const name of FIELDS) {
      expect(field(form, name).value, `Erfassen: Feld «${name}»`).toBe('');
    }
    for (const message of Object.values(MESSAGES)) {
      expect(form.textContent, 'Erfassen ohne Feldmeldung').not.toContain(message);
    }
  }

  it('zeigt den Leerzustand mit Hinweis auf CSV-Import (US-10)', async () => {
    const el = (await render([])).nativeElement as HTMLElement;

    const texts = Array.from(el.querySelectorAll('*')).map((e) =>
      e.textContent!.replace(/\s+/g, ' ').trim(),
    );
    expect(texts).toContain(EMPTY_STATE);
    expect(rows(el)).toHaveLength(0);
  });

  it('zeigt je Feature Titel, WSJF-Rang, Owner und Schätzung (US-10)', async () => {
    const el = (await render([EXPORT])).nativeElement as HTMLElement;

    const r = row(el, 'CSV-Export');
    expect(r.textContent).toContain('Beat');
    expectNumberShown(r, 2);
    expectNumberShown(r, 5);
    // Rang und Schätzung unterscheidbar, nicht nur irgendwo die Zahl.
    expect(r.textContent).toContain('#2');
    expect(r.textContent).toContain('5 SP');
  });

  it('bietet je Feature «Bearbeiten» und «Löschen» an (US-10)', async () => {
    const el = (await render([LOGIN, PORTAL])).nativeElement as HTMLElement;

    for (const r of rows(el)) {
      expect(hasButton(r, 'Bearbeiten'), r.textContent!).toBe(true);
      expect(hasButton(r, 'Löschen'), r.textContent!).toBe(true);
    }
  });

  it('füllt beim Bearbeiten das Formular in der Zeile mit den Angaben des Features (US-10)', async () => {
    const fixture = await render([LOGIN, EXPORT]);

    const form = await startEdit(fixture, 'CSV-Export');

    expect(field(form, 'Titel').value).toBe('CSV-Export');
    expect(field(form, 'Beschreibung').value).toBe('Für die Buchhaltung');
    expect(field(form, 'WSJF-Rang').value).toBe('2');
    expect(field(form, 'Owner').value).toBe('Beat');
    expect(field(form, 'Schätzung').value).toBe('5');
  });

  it('lässt optionale Felder beim Bearbeiten eines Features ohne diese Angaben leer (US-10)', async () => {
    const fixture = await render([EXPORT, LOGIN]);

    const form = await startEdit(fixture, 'Login');

    expect(field(form, 'Titel').value).toBe('Login');
    for (const name of FIELDS.filter((f) => f !== 'Titel')) {
      expect(field(form, name).value, `Feld «${name}»`).toBe('');
    }
  });

  it('speichert Rang und Schätzung des bearbeiteten Features und zeigt das neue Backlog (US-10)', async () => {
    const changed = { ...LOGIN, wsjfRank: 3, estimate: 8 };
    const fixture = await render([LOGIN], [changed]);
    repository.update.mockResolvedValue(changed);

    await editAndSubmit(fixture, 'Login', { 'WSJF-Rang': '3', Schätzung: '8' });

    expect(repository.update).toHaveBeenCalledWith(PI_ID, 'l', {
      title: 'Login',
      wsjfRank: 3,
      estimate: 8,
    });
    expect(repository.add).not.toHaveBeenCalled();
    expect(repository.list).toHaveBeenCalledTimes(2);
    const el = fixture.nativeElement as HTMLElement;
    expectNumberShown(row(el, 'Login'), 3);
    expectNumberShown(row(el, 'Login'), 8);
  });

  it('schliesst nach dem Speichern das Formular in der Zeile, das Erfassen bleibt leer (US-10)', async () => {
    const changed = { ...LOGIN, wsjfRank: 3 };
    const fixture = await render([LOGIN], [changed]);
    repository.update.mockResolvedValue(changed);

    await editAndSubmit(fixture, 'Login', { 'WSJF-Rang': '3' });

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelectorAll(EDIT_FORM)).toHaveLength(0);
    expect(hasButton(row(el, 'Login'), 'Bearbeiten')).toBe(true);
    expectAddFormUntouched(el);
  });

  it('sendet beim Bearbeiten den Titel ohne Leerzeichen am Rand und geleerte Angaben nicht mit (US-10)', async () => {
    const fixture = await render([EXPORT]);
    repository.update.mockResolvedValue(EXPORT);

    await editAndSubmit(fixture, 'CSV-Export', { Titel: '  Export  ', Owner: '', Schätzung: ' ' });

    expect(repository.update).toHaveBeenCalledTimes(1);
    const [piId, id, sent] = repository.update.mock.calls[0];
    expect(piId).toBe(PI_ID);
    expect(id).toBe('e');
    expect(sent).toEqual({ title: 'Export', description: 'Für die Buchhaltung', wsjfRank: 2 });
  });

  /** Bearbeitet `Login` mit ungültigen Angaben und gibt das Formular der Zeile zurück, das offen bleibt. */
  async function editInvalid(
    values: Partial<Record<FieldName, string>>,
  ): Promise<{ el: HTMLElement; form: HTMLFormElement }> {
    const fixture = await render([LOGIN]);
    await editAndSubmit(fixture, 'Login', values);
    const el = fixture.nativeElement as HTMLElement;
    return { el, form: editForm(el) };
  }

  it('lehnt beim Bearbeiten einen Titel aus Leerzeichen am Feld der Zeile ab und speichert nichts (US-10)', async () => {
    const { el, form } = await editInvalid({ Titel: ' ' });

    expectErrorAt(form, 'Titel', MESSAGES.titleEmpty);
    expectAddFormUntouched(el);
    expect(repository.update).not.toHaveBeenCalled();
    expect(repository.add).not.toHaveBeenCalled();
  });

  it('lehnt beim Bearbeiten einen Titel mit 201 Zeichen am Feld der Zeile ab und speichert nichts (US-10)', async () => {
    const { el, form } = await editInvalid({ Titel: 'x'.repeat(201) });

    expectErrorAt(form, 'Titel', MESSAGES.titleTooLong);
    expectAddFormUntouched(el);
    expect(repository.update).not.toHaveBeenCalled();
  });

  it('lehnt beim Bearbeiten den WSJF-Rang 0 am Feld der Zeile ab und speichert nichts (US-10)', async () => {
    const { el, form } = await editInvalid({ 'WSJF-Rang': '0' });

    expectErrorAt(form, 'WSJF-Rang', MESSAGES.rank);
    expectAddFormUntouched(el);
    expect(repository.update).not.toHaveBeenCalled();
  });

  it('lehnt beim Bearbeiten die Schätzung -1 am Feld der Zeile ab und speichert nichts (US-10)', async () => {
    const { el, form } = await editInvalid({ Schätzung: '-1' });

    expectErrorAt(form, 'Schätzung', MESSAGES.estimate);
    expectAddFormUntouched(el);
    expect(repository.update).not.toHaveBeenCalled();
  });

  it('lehnt beim Bearbeiten einen Owner mit 101 Zeichen am Feld der Zeile ab und speichert nichts (US-10)', async () => {
    const { el, form } = await editInvalid({ Owner: 'x'.repeat(101) });

    expectErrorAt(form, 'Owner', MESSAGES.ownerTooLong);
    expectAddFormUntouched(el);
    expect(repository.update).not.toHaveBeenCalled();
  });

  it('verwirft beim Abbrechen des Bearbeitens die Änderungen und erfasst danach wieder neu (US-10)', async () => {
    const fixture = await render([LOGIN], [LOGIN, PORTAL]);
    const el = fixture.nativeElement as HTMLElement;
    const form = await startEdit(fixture, 'Login');
    type(field(form, 'Titel'), 'Anmeldung');

    await click(fixture, button(editForm(el), 'Abbrechen'));

    expect(repository.update).not.toHaveBeenCalled();
    expect(el.querySelectorAll(EDIT_FORM)).toHaveLength(0);
    expect(row(el, 'Login').textContent).not.toContain('Anmeldung');
    expect(field(addForm(el), 'Titel').value).toBe('');

    repository.add.mockResolvedValue(PORTAL);
    await fillAndSubmit(fixture, { Titel: 'Self-Service-Portal' });

    expect(repository.add).toHaveBeenCalledWith(PI_ID, { title: 'Self-Service-Portal' });
    expect(repository.update).not.toHaveBeenCalled();
  });

  it('zeigt die Meldung des Servers als Alarm und behält die Angaben, wenn das Bearbeiten abgelehnt wird (US-10)', async () => {
    const fixture = await render([LOGIN]);
    repository.update.mockRejectedValue(
      new HttpErrorResponse({ status: 404, error: { message: 'Das Feature gibt es nicht.' } }),
    );

    await editAndSubmit(fixture, 'Login', { 'WSJF-Rang': '3' });

    const el = fixture.nativeElement as HTMLElement;
    const form = editForm(el);
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Das Feature gibt es nicht.');
    expect(field(form, 'Titel').value).toBe('Login');
    expect(field(form, 'WSJF-Rang').value).toBe('3');
  });

  // Szenario «Feature wird in seiner Zeile bearbeitet» (Delta #50)
  const SHOP = feature('s', 'Shop', {
    description: 'Bestellen ohne Telefon',
    wsjfRank: 4,
    owner: 'Cla',
    estimate: 13,
  });

  it('macht beim Bearbeiten die Zeile des Features zum Formular, das Erfassen bleibt leer (US-10)', async () => {
    const fixture = await render([LOGIN, SHOP]);
    const el = fixture.nativeElement as HTMLElement;

    const form = await startEdit(fixture, 'Shop');

    expect(editedRowIndex(el)).toBe(1);
    expect(rows(el)[0].querySelector('form'), 'Zeile von Login ohne Formular').toBeNull();
    expect(field(form, 'Titel').value).toBe('Shop');
    expect(field(form, 'Beschreibung').value).toBe('Bestellen ohne Telefon');
    expect(field(form, 'WSJF-Rang').value).toBe('4');
    expect(field(form, 'Owner').value).toBe('Cla');
    expect(field(form, 'Schätzung').value).toBe('13');
    expect(hasButton(form, 'Speichern')).toBe(true);
    expect(hasButton(form, 'Abbrechen')).toBe(true);
    expect(hasButton(form, 'Feature erfassen')).toBe(false);
    expectAddFormUntouched(el);
    expect(addForm(el).getAttribute('aria-label')).not.toBe('Feature bearbeiten');
  });

  it('zeigt nach dem Speichern die neuen Angaben in der Zeile des Features (US-10)', async () => {
    const changed = { ...SHOP, title: 'Webshop', owner: 'Dora', estimate: 21 };
    const fixture = await render([LOGIN, SHOP], [LOGIN, changed]);
    repository.update.mockResolvedValue(changed);
    const el = fixture.nativeElement as HTMLElement;

    await editAndSubmit(fixture, 'Shop', { Titel: 'Webshop', Owner: 'Dora', Schätzung: '21' });

    expect(repository.update).toHaveBeenCalledWith(PI_ID, 's', {
      title: 'Webshop',
      description: 'Bestellen ohne Telefon',
      wsjfRank: 4,
      owner: 'Dora',
      estimate: 21,
    });
    expect(repository.add).not.toHaveBeenCalled();
    expect(el.querySelectorAll(EDIT_FORM)).toHaveLength(0);
    const shop = rows(el)[1];
    expect(ownText(shop)).toContain('Webshop');
    expect(ownText(shop)).toContain('Dora');
    expect(ownText(shop)).toContain('21 SP');
    expect(ownText(rows(el)[0])).toContain('Login');
  });

  it('schliesst beim Bearbeiten eines zweiten Features das erste ohne zu speichern (US-10)', async () => {
    const fixture = await render([LOGIN, SHOP]);
    const el = fixture.nativeElement as HTMLElement;
    const first = await startEdit(fixture, 'Login');
    type(field(first, 'Titel'), 'Anmeldung');
    type(field(first, 'Owner'), 'Eva');

    const second = await startEdit(fixture, 'Shop');

    expect(repository.update).not.toHaveBeenCalled();
    expect(editedRowIndex(el)).toBe(1);
    expect(field(second, 'Titel').value).toBe('Shop');
    expect(field(second, 'Owner').value).toBe('Cla');
    expect(ownText(rows(el)[0])).toContain('Login');
    expect(ownText(rows(el)[0])).not.toContain('Anmeldung');
    expect(ownText(rows(el)[0])).not.toContain('Eva');

    repository.update.mockResolvedValue(SHOP);
    await saveEdit(fixture);

    expect(repository.update).toHaveBeenCalledTimes(1);
    expect(repository.update).toHaveBeenCalledWith(PI_ID, 's', {
      title: 'Shop',
      description: 'Bestellen ohne Telefon',
      wsjfRank: 4,
      owner: 'Cla',
      estimate: 13,
    });
  });

  it('löscht ein Feature erst nach der Bestätigung und zeigt dann den Leerzustand (US-10)', async () => {
    const fixture = await render([LOGIN], []);
    repository.remove.mockResolvedValue(undefined);
    const el = fixture.nativeElement as HTMLElement;

    await click(fixture, button(row(el, 'Login'), 'Löschen'));

    expect(repository.remove).not.toHaveBeenCalled();
    expect(hasButton(row(el, 'Login'), 'Wirklich löschen')).toBe(true);

    await click(fixture, button(row(el, 'Login'), 'Wirklich löschen'));

    expect(repository.remove).toHaveBeenCalledWith(PI_ID, 'l');
    expect(rows(el)).toHaveLength(0);
    expect(el.textContent).toContain(EMPTY_STATE);
  });

  it('behält das Feature, wenn das Löschen abgebrochen wird (US-10)', async () => {
    const fixture = await render([LOGIN]);
    const el = fixture.nativeElement as HTMLElement;
    await click(fixture, button(row(el, 'Login'), 'Löschen'));

    await click(fixture, button(row(el, 'Login'), 'Abbrechen'));

    expect(repository.remove).not.toHaveBeenCalled();
    expect(hasButton(row(el, 'Login'), 'Wirklich löschen')).toBe(false);
    expect(hasButton(row(el, 'Login'), 'Löschen')).toBe(true);
  });

  it('fragt nur beim gewählten Feature nach der Bestätigung (US-10)', async () => {
    const fixture = await render([PORTAL, LOGIN]);
    const el = fixture.nativeElement as HTMLElement;

    await click(fixture, button(row(el, 'Login'), 'Löschen'));

    expect(hasButton(row(el, 'Login'), 'Wirklich löschen')).toBe(true);
    expect(hasButton(row(el, 'Self-Service-Portal'), 'Wirklich löschen')).toBe(false);
  });

  it('zeigt die Meldung des Servers als Alarm und behält das Feature, wenn das Löschen scheitert (US-10)', async () => {
    const fixture = await render([LOGIN]);
    repository.remove.mockRejectedValue(
      new HttpErrorResponse({ status: 404, error: { message: 'Das Feature gibt es nicht.' } }),
    );
    const el = fixture.nativeElement as HTMLElement;
    await click(fixture, button(row(el, 'Login'), 'Löschen'));

    await click(fixture, button(row(el, 'Login'), 'Wirklich löschen'));

    expect(repository.remove).toHaveBeenCalledWith(PI_ID, 'l');
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Das Feature gibt es nicht.');
    expect(rows(el).map(ownText)).toEqual([expect.stringContaining('Login')]);
  });

  // --- US-11: Features per CSV importieren ---

  const CSV = 'Titel;WSJF-Rang;Owner;Schätzung\nPortal;1;Anna;21\nSuche;2;Beat;13\n';
  const IMPORTED_PORTAL = feature('ip', 'Portal', { wsjfRank: 1, owner: 'Anna', estimate: 21 });
  const IMPORTED_SUCHE = feature('is', 'Suche', { wsjfRank: 2, owner: 'Beat', estimate: 13 });
  const ROW_ERROR = 'Zeile 3: Der WSJF-Rang muss eine ganze Zahl ab 1 sein.';

  /** Das Datei-Feld zur Beschriftung «CSV importieren», verschachtelt oder über `for` verknüpft. */
  function csvInput(el: HTMLElement): HTMLInputElement {
    const l = Array.from(el.querySelectorAll('label')).find((x) =>
      x.textContent!.trim().startsWith('CSV importieren'),
    );
    expect(l, 'Beschriftung «CSV importieren»').toBeDefined();
    const input =
      l!.querySelector<HTMLInputElement>('input[type="file"]') ??
      (l!.htmlFor ? el.querySelector<HTMLInputElement>(`#${CSS.escape(l!.htmlFor)}`) : null);
    expect(input, 'Datei-Feld «CSV importieren»').not.toBeNull();
    expect(input!.type).toBe('file');
    return input!;
  }

  /**
   * Wählt die Dateien im Feld, wie es der Browser tut. jsdom kennt kein `DataTransfer`, darum wird `files`
   * direkt gesetzt. Das Lesen der Datei (`file.text()`) verfolgt Angular nicht; darum mehrere Runden warten.
   */
  async function choose(
    fixture: ComponentFixture<FeatureBacklogComponent>,
    ...files: File[]
  ): Promise<void> {
    const input = csvInput(fixture.nativeElement as HTMLElement);
    // Wie eine FileList: Index, `length` und `item()`.
    const fileList = Object.assign([...files], { item: (i: number) => files[i] ?? null });
    Object.defineProperty(input, 'files', { value: fileList, configurable: true });
    input.dispatchEvent(new Event('change'));
    for (let i = 0; i < 10; i++) {
      await new Promise((resolve) => setTimeout(resolve));
      await fixture.whenStable();
    }
  }

  function csvFile(content: string, name = 'backlog.csv'): File {
    return new File([content], name, { type: 'text/csv' });
  }

  function statusText(el: HTMLElement): string {
    return Array.from(el.querySelectorAll('[role="status"]'))
      .map((s) => s.textContent!.replace(/\s+/g, ' ').trim())
      .join(' | ');
  }

  it('bietet ein Datei-Feld «CSV importieren» an, das CSV-Dateien anbietet (US-11)', async () => {
    const el = (await render([])).nativeElement as HTMLElement;

    const input = csvInput(el);
    const accepted = input.accept.split(',').map((a) => a.trim());
    expect(accepted).toContain('.csv');
  });

  it('sendet den Inhalt der gewählten Datei ans gewählte PI (US-11)', async () => {
    const fixture = await render([], [IMPORTED_PORTAL, IMPORTED_SUCHE]);
    repository.importCsv.mockResolvedValue(2);

    await choose(fixture, csvFile(CSV));

    // Der Inhalt unverändert, Umlaute eingeschlossen; das Parsen ist Sache des Servers.
    expect(repository.importCsv).toHaveBeenCalledTimes(1);
    expect(repository.importCsv).toHaveBeenCalledWith(PI_ID, CSV);
  });

  it('meldet nach dem Import die Anzahl und zeigt die Features im Backlog (US-11)', async () => {
    const fixture = await render([], [IMPORTED_PORTAL, IMPORTED_SUCHE]);
    repository.importCsv.mockResolvedValue(2);

    await choose(fixture, csvFile(CSV));

    const el = fixture.nativeElement as HTMLElement;
    expect(statusText(el)).toContain('2 Features importiert.');
    expect(rows(el).map(ownText)).toEqual([
      expect.stringContaining('Portal'),
      expect.stringContaining('Suche'),
    ]);
    expect(el.querySelector('[role="alert"]')).toBeNull();
  });

  it('meldet einen einzelnen Import in der Einzahl (US-11)', async () => {
    const fixture = await render([], [IMPORTED_PORTAL]);
    repository.importCsv.mockResolvedValue(1);

    await choose(fixture, csvFile('Titel\nPortal\n'));

    const text = statusText(fixture.nativeElement as HTMLElement);
    expect(text).toContain('1 Feature importiert.');
    expect(text).not.toContain('Features importiert');
  });

  it('zeigt die Anzahl des Servers, nicht die Zeilen der Datei (US-11)', async () => {
    // Drei Datenzeilen, der Server meldet 5: die Komponente darf nicht selbst zählen.
    const fixture = await render([], [IMPORTED_PORTAL]);
    repository.importCsv.mockResolvedValue(5);

    await choose(fixture, csvFile('Titel\nA\nB\nC\n'));

    expect(statusText(fixture.nativeElement as HTMLElement)).toContain('5 Features importiert.');
  });

  it('zeigt vor dem ersten Import keine Erfolgsmeldung (US-11)', async () => {
    const el = (await render([LOGIN])).nativeElement as HTMLElement;

    expect(statusText(el)).not.toContain('importiert');
  });

  it('zeigt die Meldung des Servers als Alarm und keine Anzahl, wenn er den Import ablehnt (US-11)', async () => {
    const fixture = await render([LOGIN]);
    repository.importCsv.mockRejectedValue(
      new HttpErrorResponse({ status: 400, error: { message: ROW_ERROR } }),
    );

    await choose(fixture, csvFile('Titel;WSJF-Rang\nPortal;1\nSuche;null\n'));

    const el = fixture.nativeElement as HTMLElement;
    expect(repository.importCsv).toHaveBeenCalledTimes(1);
    expect(el.querySelector('[role="alert"]')?.textContent).toContain(ROW_ERROR);
    expect(statusText(el)).not.toContain('importiert');
    expect(rows(el).map(ownText)).toEqual([expect.stringContaining('Login')]);
  });

  it('ersetzt die Anzahl eines früheren Imports durch die Meldung, wenn ein neuer Import scheitert (US-11)', async () => {
    const fixture = await render([], [IMPORTED_PORTAL, IMPORTED_SUCHE]);
    repository.importCsv.mockResolvedValueOnce(2).mockRejectedValueOnce(
      new HttpErrorResponse({
        status: 400,
        error: { message: 'Die Kopfzeile braucht die Spalte «Titel».' },
      }),
    );
    await choose(fixture, csvFile(CSV));

    await choose(fixture, csvFile('Name;Owner\nPortal;Anna\n', 'falsch.csv'));

    const el = fixture.nativeElement as HTMLElement;
    expect(repository.importCsv).toHaveBeenCalledTimes(2);
    expect(statusText(el)).not.toContain('importiert');
    expect(el.querySelector('[role="alert"]')?.textContent).toContain(
      'Die Kopfzeile braucht die Spalte «Titel».',
    );
  });

  it('importiert nichts, wenn die Dateiauswahl ohne Datei endet (US-11)', async () => {
    const fixture = await render([]);

    await choose(fixture);

    expect(repository.importCsv).not.toHaveBeenCalled();
    expect(statusText(fixture.nativeElement as HTMLElement)).not.toContain('importiert');
  });

  // Szenario «Oberfläche nennt das CSV-Format und bietet eine Beispieldatei» (Delta #50)
  const CSV_HEADER = 'Titel;Beschreibung;WSJF-Rang;Owner;Schätzung';

  /** Inhalt einer `data:`-URL, ob prozentkodiert oder Base64. */
  function dataUrlContent(href: string): string {
    const comma = href.indexOf(',');
    const meta = href.slice(0, comma);
    const payload = href.slice(comma + 1);
    if (meta.endsWith(';base64')) {
      const bytes = Uint8Array.from(atob(payload), (c) => c.charCodeAt(0));
      return new TextDecoder().decode(bytes);
    }
    return decodeURIComponent(payload);
  }

  it('nennt beim Import die Spalten der CSV-Datei, «Titel» als Pflicht (US-11)', async () => {
    const el = (await render([])).nativeElement as HTMLElement;

    // Ausserhalb des Formulars zum Erfassen, dessen Beschriftungen dieselben Wörter tragen.
    const hints = Array.from(el.querySelectorAll<HTMLElement>('*')).filter(
      (e) =>
        !e.closest('form') &&
        !e.querySelector('form') &&
        FIELDS.every((name) => e.textContent!.includes(name)),
    );
    expect(hints.length, 'Hinweis mit allen fünf Spalten').toBeGreaterThan(0);
    expect(hints.some((h) => /Titel\s*\(Pflicht\)/.test(normalized(h)))).toBe(true);
  });

  it('bietet eine Beispieldatei mit der Kopfzeile der Spalten zum Herunterladen an (US-11)', async () => {
    const el = (await render([])).nativeElement as HTMLElement;

    const links = Array.from(el.querySelectorAll<HTMLAnchorElement>('a[download]'));
    expect(links, 'genau ein Link zum Herunterladen').toHaveLength(1);
    const link = links[0];
    expect(link.getAttribute('download')).toMatch(/\.csv$/);
    const href = link.getAttribute('href')!;
    expect(href).not.toMatch(/^unsafe:/);
    expect(href).toMatch(/^data:text\/csv[;,]/);
    expect(dataUrlContent(href).split(/\r?\n/)[0]).toBe(CSV_HEADER);
  });

  // --- US-16: Abhängigkeit zwischen Features erfassen ---

  const DEP_PORTAL = feature('dp', 'Portal', { wsjfRank: 1 });
  const DEP_LOGIN = feature('dl', 'Login');
  const DEP_ZAHLUNG = feature('dz', 'Zahlung');

  function needs(needing: Feature, needed: Feature): FeatureDependency {
    return { needingFeatureId: needing.id, neededFeatureId: needed.id };
  }

  function normalized(e: Element): string {
    return e.textContent!.replace(/\s+/g, ' ').trim();
  }

  function needsArea(r: HTMLElement): HTMLElement | null {
    return r.querySelector<HTMLElement>('[data-testid="needs"]');
  }

  function neededByArea(r: HTMLElement): HTMLElement | null {
    return r.querySelector<HTMLElement>('[data-testid="needed-by"]');
  }

  function dependencySelect(r: HTMLElement): HTMLSelectElement {
    const select = r.querySelector<HTMLSelectElement>('select[aria-label="Braucht Feature"]');
    expect(select, 'Auswahl «Braucht Feature»').not.toBeNull();
    return select!;
  }

  function removeDependencyButton(scope: HTMLElement, title: string): HTMLButtonElement {
    const found = scope.querySelector<HTMLButtonElement>(
      `button[aria-label="Abhängigkeit zu ${title} entfernen"]`,
    );
    expect(found, `Button «Abhängigkeit zu ${title} entfernen»`).not.toBeNull();
    return found!;
  }

  function alertText(el: HTMLElement): string {
    return Array.from(el.querySelectorAll('[role="alert"]'))
      .map((a) => normalized(a))
      .join(' | ');
  }

  /** Text der Alarme in der Zeile. */
  function rowAlertText(r: HTMLElement): string {
    return Array.from(r.querySelectorAll('[role="alert"]'))
      .map((a) => normalized(a))
      .join(' | ');
  }

  /** Text der Alarme ausserhalb jeder Zeile des Backlogs, etwa über der Liste. */
  function alertTextOutsideRows(el: HTMLElement): string {
    return Array.from(el.querySelectorAll('[role="alert"]'))
      .filter((a) => !a.closest('[data-testid="feature-row"]'))
      .map((a) => normalized(a))
      .join(' | ');
  }

  /** Prüft, dass die Meldung als Alarm in der Zeile steht und weder über der Liste noch in einer anderen Zeile. */
  function expectAlertOnlyInRow(el: HTMLElement, title: string, message: string) {
    expect(rowAlertText(row(el, title)), `Alarm in der Zeile von ${title}`).toContain(message);
    expect(alertTextOutsideRows(el), 'Alarm über der Liste').not.toContain(message);
    for (const other of rows(el).filter((r) => r !== row(el, title))) {
      expect(rowAlertText(other), `Alarm in einer anderen Zeile: ${ownText(other)}`).not.toContain(
        message,
      );
    }
  }

  /** Der Button «Abhängigkeit hinzufügen» der Zeile. */
  function addDependencyButton(r: HTMLElement): HTMLButtonElement {
    return button(r, 'Abhängigkeit hinzufügen');
  }

  /** Wartet, bis Aktion und anschliessendes Neuladen durch sind (mehrere Runden asynchroner Aufrufe). */
  async function settle(fixture: ComponentFixture<FeatureBacklogComponent>): Promise<void> {
    for (let i = 0; i < 5; i++) {
      await new Promise((resolve) => setTimeout(resolve));
      await fixture.whenStable();
    }
  }

  async function chooseNeeded(
    fixture: ComponentFixture<FeatureBacklogComponent>,
    r: HTMLElement,
    featureId: string,
  ): Promise<void> {
    const select = dependencySelect(r);
    select.value = featureId;
    select.dispatchEvent(new Event('change'));
    await fixture.whenStable();
  }

  it('lädt die Abhängigkeiten des PI zusätzlich zum Backlog (US-16)', async () => {
    await render([DEP_PORTAL, DEP_LOGIN]);

    expect(dependencyRepository.list).toHaveBeenCalledWith(PI_ID);
    expect(repository.list).toHaveBeenCalledWith(PI_ID);
  });

  it('zeigt beim Feature, welches Feature es braucht (US-16)', async () => {
    dependencyLists = [[needs(DEP_PORTAL, DEP_LOGIN)]];
    const fixture = await render([DEP_PORTAL, DEP_LOGIN]);
    await settle(fixture);
    const el = fixture.nativeElement as HTMLElement;

    const area = needsArea(row(el, 'Portal'));
    expect(area, 'Angabe «Braucht» bei Portal').not.toBeNull();
    expect(normalized(area!)).toMatch(/^Braucht: Login\b/);
    expect(needsArea(row(el, 'Login')), 'Login braucht nichts').toBeNull();
  });

  it('zeigt beim gebrauchten Feature, von welchem Feature es gebraucht wird (US-16)', async () => {
    dependencyLists = [[needs(DEP_PORTAL, DEP_LOGIN)]];
    const fixture = await render([DEP_PORTAL, DEP_LOGIN]);
    await settle(fixture);
    const el = fixture.nativeElement as HTMLElement;

    const area = neededByArea(row(el, 'Login'));
    expect(area, 'Angabe «Wird gebraucht von» bei Login').not.toBeNull();
    expect(normalized(area!)).toMatch(/^Wird gebraucht von: Portal\b/);
    expect(neededByArea(row(el, 'Portal')), 'Portal wird von niemandem gebraucht').toBeNull();
  });

  it('zeigt bei einem Feature beide Richtungen getrennt (US-16)', async () => {
    // Portal braucht Login, Login braucht Zahlung: Login steht in der Mitte.
    dependencyLists = [[needs(DEP_PORTAL, DEP_LOGIN), needs(DEP_LOGIN, DEP_ZAHLUNG)]];
    const fixture = await render([DEP_PORTAL, DEP_LOGIN, DEP_ZAHLUNG]);
    await settle(fixture);
    const login = row(fixture.nativeElement as HTMLElement, 'Login');

    const needed = normalized(needsArea(login)!);
    const neededBy = normalized(neededByArea(login)!);
    expect(needed).toContain('Braucht:');
    expect(needed).toContain('Zahlung');
    expect(needed).not.toContain('Portal');
    expect(neededBy).toContain('Wird gebraucht von:');
    expect(neededBy).toContain('Portal');
    expect(neededBy).not.toContain('Zahlung');
  });

  it('nennt alle Features, die ein Feature braucht (US-16)', async () => {
    dependencyLists = [[needs(DEP_PORTAL, DEP_LOGIN), needs(DEP_PORTAL, DEP_ZAHLUNG)]];
    const fixture = await render([DEP_PORTAL, DEP_LOGIN, DEP_ZAHLUNG]);
    await settle(fixture);
    const el = fixture.nativeElement as HTMLElement;

    const needed = normalized(needsArea(row(el, 'Portal'))!);
    expect(needed).toContain('Login');
    expect(needed).toContain('Zahlung');
    expect(normalized(neededByArea(row(el, 'Login'))!)).toContain('Portal');
    expect(normalized(neededByArea(row(el, 'Zahlung'))!)).toContain('Portal');
  });

  it('zeigt ohne Abhängigkeiten weder «Braucht» noch «Wird gebraucht von» (US-16)', async () => {
    const fixture = await render([DEP_PORTAL, DEP_LOGIN]);
    await settle(fixture);
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelectorAll('[data-testid="needs"]')).toHaveLength(0);
    expect(el.querySelectorAll('[data-testid="needed-by"]')).toHaveLength(0);
  });

  it('bietet je gebrauchtem Feature einen Button «Entfernen» an (US-16)', async () => {
    dependencyLists = [[needs(DEP_PORTAL, DEP_LOGIN), needs(DEP_PORTAL, DEP_ZAHLUNG)]];
    const fixture = await render([DEP_PORTAL, DEP_LOGIN, DEP_ZAHLUNG]);
    await settle(fixture);
    const portal = row(fixture.nativeElement as HTMLElement, 'Portal');

    expect(removeDependencyButton(portal, 'Login').textContent!.trim()).toBe('Entfernen');
    expect(removeDependencyButton(portal, 'Zahlung').textContent!.trim()).toBe('Entfernen');
  });

  it('entfernt eine Abhängigkeit und zeigt sie danach nicht mehr (US-16)', async () => {
    dependencyLists = [[needs(DEP_PORTAL, DEP_LOGIN)], []];
    const fixture = await render([DEP_PORTAL, DEP_LOGIN]);
    await settle(fixture);
    dependencyRepository.remove.mockResolvedValue(undefined);
    const el = fixture.nativeElement as HTMLElement;

    await click(fixture, removeDependencyButton(row(el, 'Portal'), 'Login'));
    await settle(fixture);

    expect(dependencyRepository.remove).toHaveBeenCalledWith(PI_ID, 'dp', 'dl');
    expect(dependencyRepository.list).toHaveBeenCalledTimes(2);
    expect(needsArea(row(el, 'Portal'))).toBeNull();
    expect(neededByArea(row(el, 'Login'))).toBeNull();
  });

  it('entfernt nur die gewählte Abhängigkeit (US-16)', async () => {
    dependencyLists = [[needs(DEP_PORTAL, DEP_LOGIN), needs(DEP_PORTAL, DEP_ZAHLUNG)]];
    const fixture = await render([DEP_PORTAL, DEP_LOGIN, DEP_ZAHLUNG]);
    await settle(fixture);
    dependencyRepository.remove.mockResolvedValue(undefined);
    const el = fixture.nativeElement as HTMLElement;

    await click(fixture, removeDependencyButton(row(el, 'Portal'), 'Zahlung'));
    await settle(fixture);

    expect(dependencyRepository.remove).toHaveBeenCalledTimes(1);
    expect(dependencyRepository.remove).toHaveBeenCalledWith(PI_ID, 'dp', 'dz');
  });

  it('zeigt die Meldung des Servers als Alarm, wenn das Entfernen scheitert (US-16)', async () => {
    dependencyLists = [[needs(DEP_PORTAL, DEP_LOGIN)]];
    const fixture = await render([DEP_PORTAL, DEP_LOGIN]);
    await settle(fixture);
    const message = 'Meldung des Servers zum Entfernen';
    dependencyRepository.remove.mockRejectedValue(
      new HttpErrorResponse({ status: 404, error: { message } }),
    );
    const el = fixture.nativeElement as HTMLElement;

    await click(fixture, removeDependencyButton(row(el, 'Portal'), 'Login'));
    await settle(fixture);

    expect(alertText(el)).toContain(message);
    expectAlertOnlyInRow(el, 'Portal', message);
    expect(normalized(needsArea(row(el, 'Portal'))!)).toContain('Login');
  });

  it('bietet je Feature die anderen Features des Backlogs zur Auswahl an (US-16)', async () => {
    const fixture = await render([DEP_PORTAL, DEP_LOGIN, DEP_ZAHLUNG]);
    await settle(fixture);
    const select = dependencySelect(row(fixture.nativeElement as HTMLElement, 'Portal'));

    const options = Array.from(select.options).map((o) => ({
      value: o.value,
      text: o.textContent!.trim(),
    }));
    expect(
      options.filter((o) => o.value === ''),
      'Platzhalter ohne Wert',
    ).toEqual([{ value: '', text: 'Feature wählen …' }]);
    expect(select.value, 'ohne Wahl steht der Platzhalter').toBe('');
    expect(
      options.filter((o) => o.value !== '').sort((a, b) => a.value.localeCompare(b.value)),
    ).toEqual([
      { value: 'dl', text: 'Login' },
      { value: 'dz', text: 'Zahlung' },
    ]);
  });

  it('hält fest, dass ein Feature das gewählte Feature braucht, und zeigt es danach an (US-16)', async () => {
    dependencyLists = [[], [needs(DEP_PORTAL, DEP_LOGIN)]];
    const fixture = await render([DEP_PORTAL, DEP_LOGIN]);
    await settle(fixture);
    dependencyRepository.add.mockResolvedValue(needs(DEP_PORTAL, DEP_LOGIN));
    const el = fixture.nativeElement as HTMLElement;

    await chooseNeeded(fixture, row(el, 'Portal'), 'dl');
    await click(fixture, button(row(el, 'Portal'), 'Abhängigkeit hinzufügen'));
    await settle(fixture);

    expect(dependencyRepository.add).toHaveBeenCalledTimes(1);
    expect(dependencyRepository.add).toHaveBeenCalledWith(PI_ID, 'dp', 'dl');
    expect(normalized(needsArea(row(el, 'Portal'))!)).toMatch(/^Braucht: Login\b/);
    expect(normalized(neededByArea(row(el, 'Login'))!)).toMatch(/^Wird gebraucht von: Portal\b/);
  });

  it('hält die Abhängigkeit für das Feature der Zeile fest, in der gewählt wird (US-16)', async () => {
    const fixture = await render([DEP_PORTAL, DEP_LOGIN, DEP_ZAHLUNG]);
    await settle(fixture);
    dependencyRepository.add.mockResolvedValue(needs(DEP_LOGIN, DEP_ZAHLUNG));
    const el = fixture.nativeElement as HTMLElement;

    await chooseNeeded(fixture, row(el, 'Login'), 'dz');
    await click(fixture, button(row(el, 'Login'), 'Abhängigkeit hinzufügen'));
    await settle(fixture);

    expect(dependencyRepository.add).toHaveBeenCalledTimes(1);
    expect(dependencyRepository.add).toHaveBeenCalledWith(PI_ID, 'dl', 'dz');
  });

  it('hält ohne Auswahl keine Abhängigkeit fest (US-16)', async () => {
    const fixture = await render([DEP_PORTAL, DEP_LOGIN]);
    await settle(fixture);
    const el = fixture.nativeElement as HTMLElement;

    expect(addDependencyButton(row(el, 'Portal')).disabled, 'ohne Wahl nicht aktiv').toBe(true);

    await click(fixture, addDependencyButton(row(el, 'Portal')));
    await settle(fixture);

    expect(dependencyRepository.add).not.toHaveBeenCalled();

    await chooseNeeded(fixture, row(el, 'Portal'), 'dl');

    expect(addDependencyButton(row(el, 'Portal')).disabled, 'mit Wahl aktiv').toBe(false);
  });

  it('zeigt die Meldung des Servers als Alarm, wenn er die Abhängigkeit ablehnt (US-16)', async () => {
    // Seit Delta #50 bietet die Auswahl verbundene Features nicht mehr an; abgelehnt wird hier, weil jemand
    // anderes die umgekehrte Abhängigkeit inzwischen erfasst hat.
    const fixture = await render([DEP_PORTAL, DEP_LOGIN]);
    await settle(fixture);
    dependencyRepository.add.mockRejectedValue(
      new HttpErrorResponse({
        status: 400,
        error: { message: 'Die beiden Features hängen schon umgekehrt voneinander ab.' },
      }),
    );
    const el = fixture.nativeElement as HTMLElement;
    expect(alertText(el)).toBe('');

    await chooseNeeded(fixture, row(el, 'Login'), 'dp');
    await click(fixture, button(row(el, 'Login'), 'Abhängigkeit hinzufügen'));
    await settle(fixture);

    expect(dependencyRepository.add).toHaveBeenCalledWith(PI_ID, 'dl', 'dp');
    expect(alertText(el)).toContain('Die beiden Features hängen schon umgekehrt voneinander ab.');
    expectAlertOnlyInRow(el, 'Login', 'Die beiden Features hängen schon umgekehrt voneinander ab.');
  });

  it('lädt nach dem Löschen eines Features dessen Abhängigkeiten neu, sie verschwinden (US-16)', async () => {
    // Szenario «Gelöschtes Feature nimmt seine Abhängigkeiten mit»: der Server löscht sie mit.
    dependencyLists = [[needs(DEP_PORTAL, DEP_LOGIN)], []];
    const fixture = await render([DEP_PORTAL, DEP_LOGIN], [DEP_PORTAL]);
    await settle(fixture);
    repository.remove.mockResolvedValue(undefined);
    const el = fixture.nativeElement as HTMLElement;
    expect(needsArea(row(el, 'Portal')), 'vor dem Löschen').not.toBeNull();

    await click(fixture, button(row(el, 'Login'), 'Löschen'));
    await click(fixture, button(row(el, 'Login'), 'Wirklich löschen'));
    await settle(fixture);

    expect(repository.remove).toHaveBeenCalledWith(PI_ID, 'dl');
    expect(dependencyRepository.list).toHaveBeenCalledTimes(2);
    expect(dependencyRepository.list).toHaveBeenLastCalledWith(PI_ID);
    expect(rows(el).map(ownText)).toEqual([expect.stringContaining('Portal')]);
    expect(needsArea(row(el, 'Portal'))).toBeNull();
  });

  // Szenarien «Auswahl bietet nur noch nicht verbundene Features an» und «Abgelehnte Abhängigkeit meldet sich
  // beim Feature» (Delta #50)
  const DEP_SHOP = feature('ds', 'Shop');
  const DEP_KARTE = feature('dk', 'Karte');
  const DEP_SUCHE = feature('du', 'Suche');
  const REJECTED = 'Meldung des Servers zur abgelehnten Abhängigkeit';
  const REJECTED_OTHER = 'Zweite Meldung des Servers';

  function rejection(message: string): HttpErrorResponse {
    return new HttpErrorResponse({ status: 400, error: { message } });
  }

  /** Titel der wählbaren Features der Auswahl, ohne Platzhalter, sortiert. */
  function choices(r: HTMLElement): string[] {
    return Array.from(dependencySelect(r).options)
      .filter((o) => o.value !== '')
      .map((o) => o.textContent!.trim())
      .sort();
  }

  async function addNeeded(
    fixture: ComponentFixture<FeatureBacklogComponent>,
    title: string,
    neededId: string,
  ): Promise<void> {
    const el = fixture.nativeElement as HTMLElement;
    await chooseNeeded(fixture, row(el, title), neededId);
    await click(fixture, addDependencyButton(row(el, title)));
    await settle(fixture);
  }

  it('bietet in der Auswahl nur Features an, mit denen das Feature in keiner Richtung verbunden ist (US-16)', async () => {
    // Shop braucht Login, Karte braucht Shop; Suche braucht Login, ist mit Shop aber nicht verbunden.
    dependencyLists = [
      [needs(DEP_SHOP, DEP_LOGIN), needs(DEP_KARTE, DEP_SHOP), needs(DEP_SUCHE, DEP_LOGIN)],
    ];
    const fixture = await render([DEP_SHOP, DEP_LOGIN, DEP_KARTE, DEP_SUCHE]);
    await settle(fixture);
    const el = fixture.nativeElement as HTMLElement;

    expect(choices(row(el, 'Shop')), 'bei Shop').toEqual(['Suche']);
    // Nur direkt verbundene fallen weg: Karte braucht Login nicht selbst.
    expect(choices(row(el, 'Karte')), 'bei Karte').toEqual(['Login', 'Suche']);
    expect(choices(row(el, 'Login')), 'bei Login').toEqual(['Karte']);
  });

  it('zeigt ohne Wahl den Platzhalter «Feature wählen …», «Abhängigkeit hinzufügen» ist nicht aktiv (US-16)', async () => {
    dependencyLists = [[needs(DEP_SHOP, DEP_LOGIN)]];
    const fixture = await render([DEP_SHOP, DEP_LOGIN, DEP_SUCHE]);
    await settle(fixture);
    const shop = row(fixture.nativeElement as HTMLElement, 'Shop');

    const select = dependencySelect(shop);
    expect(select.value).toBe('');
    expect(select.selectedOptions[0]?.textContent!.trim()).toBe('Feature wählen …');
    expect(addDependencyButton(shop).disabled).toBe(true);
  });

  it('sperrt «Abhängigkeit hinzufügen» wieder, wenn das gewählte Feature aus der Auswahl fällt (US-16)', async () => {
    // Bei Shop ist Suche gewählt, aber nicht hinzugefügt; dann erfasst Suche, dass es Shop braucht.
    dependencyLists = [[], [needs(DEP_SUCHE, DEP_SHOP)]];
    const fixture = await render([DEP_SHOP, DEP_SUCHE]);
    await settle(fixture);
    dependencyRepository.add.mockResolvedValue(needs(DEP_SUCHE, DEP_SHOP));
    const el = fixture.nativeElement as HTMLElement;
    await chooseNeeded(fixture, row(el, 'Shop'), 'du');
    expect(addDependencyButton(row(el, 'Shop')).disabled).toBe(false);

    await addNeeded(fixture, 'Suche', 'ds');

    const shop = row(el, 'Shop');
    expect(choices(shop)).toEqual([]);
    expect(dependencySelect(shop).value).toBe('');
    expect(addDependencyButton(shop).disabled).toBe(true);
  });

  it('zeigt eine abgelehnte Abhängigkeit in der Zeile des Features, nicht über der Liste (US-16)', async () => {
    const fixture = await render([DEP_SHOP, DEP_SUCHE]);
    await settle(fixture);
    dependencyRepository.add.mockRejectedValue(rejection(REJECTED));
    const el = fixture.nativeElement as HTMLElement;

    await addNeeded(fixture, 'Shop', 'du');

    expect(dependencyRepository.add).toHaveBeenCalledWith(PI_ID, 'ds', 'du');
    expectAlertOnlyInRow(el, 'Shop', REJECTED);
  });

  it('lässt die Meldung verschwinden, sobald in einer anderen Zeile eine Abhängigkeit erfasst wird (US-16)', async () => {
    dependencyLists = [[], [needs(DEP_SUCHE, DEP_SHOP)]];
    const fixture = await render([DEP_SHOP, DEP_SUCHE]);
    await settle(fixture);
    dependencyRepository.add
      .mockRejectedValueOnce(rejection(REJECTED))
      .mockResolvedValueOnce(needs(DEP_SUCHE, DEP_SHOP));
    const el = fixture.nativeElement as HTMLElement;
    await addNeeded(fixture, 'Shop', 'du');
    expectAlertOnlyInRow(el, 'Shop', REJECTED);

    await addNeeded(fixture, 'Suche', 'ds');

    expect(dependencyRepository.add).toHaveBeenLastCalledWith(PI_ID, 'du', 'ds');
    expect(alertText(el)).not.toContain(REJECTED);
  });

  it('lässt die Meldung verschwinden, sobald in einer anderen Zeile eine Abhängigkeit entfernt wird (US-16)', async () => {
    dependencyLists = [[needs(DEP_LOGIN, DEP_SUCHE)], []];
    const fixture = await render([DEP_SHOP, DEP_LOGIN, DEP_SUCHE]);
    await settle(fixture);
    dependencyRepository.add.mockRejectedValue(rejection(REJECTED));
    dependencyRepository.remove.mockResolvedValue(undefined);
    const el = fixture.nativeElement as HTMLElement;
    await addNeeded(fixture, 'Shop', 'du');
    expectAlertOnlyInRow(el, 'Shop', REJECTED);

    await click(fixture, removeDependencyButton(row(el, 'Login'), 'Suche'));
    await settle(fixture);

    expect(dependencyRepository.remove).toHaveBeenCalledWith(PI_ID, 'dl', 'du');
    expect(alertText(el)).not.toContain(REJECTED);
  });

  it('ersetzt die Meldung, wenn die nächste Aktion in einer anderen Zeile ebenfalls scheitert (US-16)', async () => {
    const fixture = await render([DEP_SHOP, DEP_SUCHE]);
    await settle(fixture);
    dependencyRepository.add
      .mockRejectedValueOnce(rejection(REJECTED))
      .mockRejectedValueOnce(rejection(REJECTED_OTHER));
    const el = fixture.nativeElement as HTMLElement;
    await addNeeded(fixture, 'Shop', 'du');
    expectAlertOnlyInRow(el, 'Shop', REJECTED);

    await addNeeded(fixture, 'Suche', 'ds');

    expect(alertText(el)).not.toContain(REJECTED);
    expectAlertOnlyInRow(el, 'Suche', REJECTED_OTHER);
  });

  it('lässt die Meldung eines gescheiterten Entfernens bei der nächsten Aktion verschwinden (US-16)', async () => {
    dependencyLists = [
      [needs(DEP_PORTAL, DEP_LOGIN)],
      [needs(DEP_PORTAL, DEP_LOGIN), needs(DEP_ZAHLUNG, DEP_LOGIN)],
    ];
    const fixture = await render([DEP_PORTAL, DEP_LOGIN, DEP_ZAHLUNG]);
    await settle(fixture);
    dependencyRepository.remove.mockRejectedValue(rejection(REJECTED));
    dependencyRepository.add.mockResolvedValue(needs(DEP_ZAHLUNG, DEP_LOGIN));
    const el = fixture.nativeElement as HTMLElement;
    await click(fixture, removeDependencyButton(row(el, 'Portal'), 'Login'));
    await settle(fixture);
    expectAlertOnlyInRow(el, 'Portal', REJECTED);

    await addNeeded(fixture, 'Zahlung', 'dl');

    expect(dependencyRepository.add).toHaveBeenCalledWith(PI_ID, 'dz', 'dl');
    expect(alertText(el)).not.toContain(REJECTED);
  });
});
