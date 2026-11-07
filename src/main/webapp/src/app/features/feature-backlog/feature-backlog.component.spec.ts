import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Feature } from '../../core/models/feature.model';
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
  };

  /** Rendert das Backlog des PI; `lists` sind die Antworten des Servers auf das erste, zweite, … Laden. */
  async function render(...lists: Feature[][]): Promise<ComponentFixture<FeatureBacklogComponent>> {
    const list = vi.fn();
    for (const answer of lists) {
      list.mockResolvedValueOnce(answer);
    }
    list.mockResolvedValue(lists.at(-1) ?? []);
    repository = { list, add: vi.fn(), update: vi.fn(), remove: vi.fn() };
    TestBed.configureTestingModule({
      imports: [FeatureBacklogComponent],
      providers: [{ provide: FeaturesRepository, useValue: repository }],
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

    expect(rows(el).map((r) => r.textContent)).toEqual([
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
    expect(rows(el).map((r) => r.textContent)).toEqual([
      expect.stringContaining('Self-Service-Portal'),
    ]);
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

  function row(el: HTMLElement, title: string): HTMLElement {
    const found = rows(el).find((r) => r.textContent!.includes(title));
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

  /**
   * Das Formular des Features, ob zum Erfassen oder Bearbeiten: Bearbeiten nutzt dasselbe Formular, dessen
   * Absende-Button dann anders heissen darf. Erkennbar an der Beschriftung «Titel».
   */
  function featureForm(el: HTMLElement): HTMLFormElement {
    const forms = Array.from(el.querySelectorAll('form')).filter((f) =>
      Array.from(f.querySelectorAll('label')).some((l) =>
        l.textContent!.trim().startsWith('Titel'),
      ),
    );
    expect(forms, 'genau ein Formular mit dem Feld «Titel»').toHaveLength(1);
    return forms[0];
  }

  /** Klickt den einzigen Absende-Button des Formulars, unabhängig von seiner Beschriftung. */
  async function submitFeatureForm(fixture: ComponentFixture<FeatureBacklogComponent>) {
    const form = featureForm(fixture.nativeElement as HTMLElement);
    const submits = Array.from(form.querySelectorAll('button')).filter((b) => b.type === 'submit');
    expect(submits, 'genau ein Absende-Button').toHaveLength(1);
    await click(fixture, submits[0]);
  }

  async function startEdit(
    fixture: ComponentFixture<FeatureBacklogComponent>,
    title: string,
  ): Promise<HTMLFormElement> {
    const el = fixture.nativeElement as HTMLElement;
    await click(fixture, button(row(el, title), 'Bearbeiten'));
    return featureForm(el);
  }

  async function editAndSubmit(
    fixture: ComponentFixture<FeatureBacklogComponent>,
    title: string,
    values: Partial<Record<FieldName, string>>,
  ): Promise<HTMLFormElement> {
    const el = fixture.nativeElement as HTMLElement;
    await startEdit(fixture, title);
    for (const [name, value] of Object.entries(values) as [FieldName, string][]) {
      type(field(featureForm(el), name), value);
    }
    await submitFeatureForm(fixture);
    return featureForm(el);
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

  it('füllt beim Bearbeiten das Formular mit den Angaben des Features (US-10)', async () => {
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

  it('kehrt nach dem Speichern zum Erfassen zurück (US-10)', async () => {
    const changed = { ...LOGIN, wsjfRank: 3 };
    const fixture = await render([LOGIN], [changed]);
    repository.update.mockResolvedValue(changed);

    await editAndSubmit(fixture, 'Login', { 'WSJF-Rang': '3' });

    const form = addForm(fixture.nativeElement as HTMLElement);
    for (const name of FIELDS) {
      expect(field(form, name).value, `Feld «${name}»`).toBe('');
    }
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

  it('lehnt beim Bearbeiten einen Titel aus Leerzeichen am Feld ab und speichert nichts (US-10)', async () => {
    const form = await editAndSubmit(await render([LOGIN]), 'Login', { Titel: ' ' });

    expectErrorAt(form, 'Titel', MESSAGES.titleEmpty);
    expect(repository.update).not.toHaveBeenCalled();
    expect(repository.add).not.toHaveBeenCalled();
  });

  it('lehnt beim Bearbeiten einen Titel mit 201 Zeichen am Feld ab und speichert nichts (US-10)', async () => {
    const form = await editAndSubmit(await render([LOGIN]), 'Login', { Titel: 'x'.repeat(201) });

    expectErrorAt(form, 'Titel', MESSAGES.titleTooLong);
    expect(repository.update).not.toHaveBeenCalled();
  });

  it('lehnt beim Bearbeiten den WSJF-Rang 0 am Feld ab und speichert nichts (US-10)', async () => {
    const form = await editAndSubmit(await render([LOGIN]), 'Login', { 'WSJF-Rang': '0' });

    expectErrorAt(form, 'WSJF-Rang', MESSAGES.rank);
    expect(repository.update).not.toHaveBeenCalled();
  });

  it('lehnt beim Bearbeiten die Schätzung -1 am Feld ab und speichert nichts (US-10)', async () => {
    const form = await editAndSubmit(await render([LOGIN]), 'Login', { Schätzung: '-1' });

    expectErrorAt(form, 'Schätzung', MESSAGES.estimate);
    expect(repository.update).not.toHaveBeenCalled();
  });

  it('lehnt beim Bearbeiten einen Owner mit 101 Zeichen am Feld ab und speichert nichts (US-10)', async () => {
    const form = await editAndSubmit(await render([LOGIN]), 'Login', { Owner: 'x'.repeat(101) });

    expectErrorAt(form, 'Owner', MESSAGES.ownerTooLong);
    expect(repository.update).not.toHaveBeenCalled();
  });

  it('verwirft beim Abbrechen des Bearbeitens die Änderungen und erfasst danach wieder neu (US-10)', async () => {
    const fixture = await render([LOGIN], [LOGIN, PORTAL]);
    const el = fixture.nativeElement as HTMLElement;
    const form = await startEdit(fixture, 'Login');
    type(field(form, 'Titel'), 'Anmeldung');

    await click(fixture, button(featureForm(el), 'Abbrechen'));

    expect(repository.update).not.toHaveBeenCalled();
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

    const form = await editAndSubmit(fixture, 'Login', { 'WSJF-Rang': '3' });

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Das Feature gibt es nicht.');
    expect(field(form, 'Titel').value).toBe('Login');
    expect(field(form, 'WSJF-Rang').value).toBe('3');
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
    expect(rows(el).map((r) => r.textContent)).toEqual([expect.stringContaining('Login')]);
  });
});
