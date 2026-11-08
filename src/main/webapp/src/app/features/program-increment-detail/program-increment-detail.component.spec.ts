import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { RouterTestingHarness } from '@angular/router/testing';
import { appConfig } from '../../app.config';
import { ProgramIncrement } from '../../core/models/program-increment.model';
import { FeaturesRepository } from '../../core/ports/features.repository';
import { CapacitiesRepository } from '../../core/ports/capacities.repository';
import { MilestonesRepository } from '../../core/ports/milestones.repository';
import { ProgramIncrementsRepository } from '../../core/ports/program-increments.repository';
import { TeamsRepository } from '../../core/ports/teams.repository';

/** PI 27.1 (04.01.–26.03.2027) mit Iterationen zu 2 Wochen, wie es der Server liefert (US-5). */
const PI_27_1: ProgramIncrement = {
  id: '1',
  name: 'PI 27.1',
  startDate: '2027-01-04',
  endDate: '2027-03-26',
  iterationLengthWeeks: 2,
  iterations: [
    ['Iteration 1', '2027-01-04', '2027-01-17'],
    ['Iteration 2', '2027-01-18', '2027-01-31'],
    ['Iteration 3', '2027-02-01', '2027-02-14'],
    ['Iteration 4', '2027-02-15', '2027-02-28'],
    ['Iteration 5', '2027-03-01', '2027-03-14'],
    ['IP', '2027-03-15', '2027-03-26'],
  ].map(([name, startDate, endDate], i) => ({
    number: i + 1,
    name,
    startDate,
    endDate,
    innovationAndPlanning: name === 'IP',
  })),
  businessContext: '',
  artObjectives: '',
  links: [],
};

const VISION = { id: 'l1', title: 'Vision', url: 'https://wiki.example.ch/vision' };
const ROADMAP = { id: 'l2', title: 'Roadmap', url: 'http://wiki.example.ch/roadmap' };
const EMPTY_HINT = 'Noch kein Business-Kontext erfasst.';

describe('ProgramIncrementDetailComponent', () => {
  let get: ReturnType<typeof vi.fn>;
  let updateBusinessContext: ReturnType<typeof vi.fn>;
  let addLink: ReturnType<typeof vi.fn>;
  let removeLink: ReturnType<typeof vi.fn>;
  let harness: RouterTestingHarness;
  let listFeatures: ReturnType<typeof vi.fn>;
  let listMilestones: ReturnType<typeof vi.fn>;

  /** Öffnet die Detailseite über die echte Route, damit die ID aus der URL kommt wie in der App. */
  async function open(url: string): Promise<HTMLElement> {
    TestBed.configureTestingModule({
      providers: [
        ...appConfig.providers,
        {
          provide: ProgramIncrementsRepository,
          useValue: {
            list: vi.fn(),
            get,
            plan: vi.fn(),
            updateBusinessContext,
            addLink,
            removeLink,
          },
        },
        // Das eingebettete Backlog (US-9) darf im Test keinen HTTP-Aufruf machen.
        { provide: FeaturesRepository, useValue: { list: listFeatures, add: vi.fn() } },
        // Die eingebettete Kapazitätstabelle (US-7) lädt Teams und Kapazitäten; ohne Mocks ginge sie ans Netz.
        {
          provide: TeamsRepository,
          useValue: {
            list: vi.fn().mockResolvedValue([]),
            add: vi.fn(),
            rename: vi.fn(),
            remove: vi.fn(),
          },
        },
        {
          provide: CapacitiesRepository,
          useValue: { list: vi.fn().mockResolvedValue([]), set: vi.fn() },
        },
        // Der eingebettete Milestone-Abschnitt (US-15) lädt die Milestones; ohne Mock ginge er ans Netz.
        {
          provide: MilestonesRepository,
          useValue: { list: listMilestones, add: vi.fn(), update: vi.fn(), remove: vi.fn() },
        },
      ],
    });
    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url);
    await settle();
    return harness.routeNativeElement as HTMLElement;
  }

  async function settle(): Promise<void> {
    await harness.fixture.whenStable();
    harness.detectChanges();
  }

  function button(scope: Element, text: string): HTMLButtonElement | undefined {
    return Array.from(scope.querySelectorAll('button')).find((b) =>
      (b.textContent ?? '').includes(text),
    );
  }

  /**
   * Der Abschnitt «Business-Kontext». Die Suche bleibt darin, weil das eingebettete Backlog eigene Knöpfe
   * «Bearbeiten» und ein eigenes Feld `title` hat.
   */
  function contextSection(el: HTMLElement): HTMLElement {
    const heading = Array.from(el.querySelectorAll('h2')).find(
      (h) => (h.textContent ?? '').trim() === 'Business-Kontext',
    );
    const section = heading?.closest('section');
    expect(section, 'Abschnitt «Business-Kontext»').toBeTruthy();
    return section as HTMLElement;
  }

  /** Drückt im Abschnitt «Business-Kontext» den Knopf mit dieser Beschriftung; er muss da sein. */
  async function press(el: HTMLElement, text: string): Promise<void> {
    const found = button(contextSection(el), text);
    expect(found, `Knopf «${text}»`).toBeDefined();
    found!.click();
    await settle();
  }

  /** Öffnet das Formular für Business-Kontext und PI Objectives: mit Inhalt «Bearbeiten», ohne «Erfassen». */
  async function openContextForm(el: HTMLElement, label: 'Bearbeiten' | 'Erfassen'): Promise<void> {
    expect(textareas(el), 'Formular vor dem Öffnen').toEqual([]);
    await press(el, label);
  }

  /** Öffnet das Formular für Links über den Knopf «Link hinzufügen». */
  async function openLinkForm(el: HTMLElement): Promise<void> {
    expect(contextSection(el).querySelector('input'), 'Link-Formular vor dem Öffnen').toBeNull();
    await press(el, 'Link hinzufügen');
  }

  function textareas(el: HTMLElement): HTMLTextAreaElement[] {
    return Array.from(contextSection(el).querySelectorAll('textarea'));
  }

  function textarea(el: HTMLElement, name: string): HTMLTextAreaElement {
    const found = contextSection(el).querySelector(`textarea[formControlName="${name}"]`);
    expect(found, `Eingabefeld ${name}`).not.toBeNull();
    return found as HTMLTextAreaElement;
  }

  function linkInput(el: HTMLElement, name: string): HTMLInputElement {
    const found = contextSection(el).querySelector(`input[formControlName="${name}"]`);
    expect(found, `Eingabefeld ${name}`).not.toBeNull();
    return found as HTMLInputElement;
  }

  function occurrences(text: string, part: string): number {
    return text.split(part).length - 1;
  }

  function type(field: HTMLInputElement | HTMLTextAreaElement, value: string): void {
    field.value = value;
    field.dispatchEvent(new Event('input'));
  }

  async function submit(field: Element): Promise<void> {
    field.closest('form')!.dispatchEvent(new Event('submit'));
    await settle();
  }

  function shown(el: HTMLElement, testId: string): HTMLElement | null {
    return el.querySelector(`[data-testid="${testId}"]`);
  }

  /** Das kleinste Element um den Link, das einen Knopf enthält: die Zeile des Links. */
  function linkRow(anchor: Element): Element {
    let row: Element | null = anchor.parentElement;
    while (row && !row.querySelector('button')) {
      row = row.parentElement;
    }
    expect(row, 'Zeile des Links mit Knopf').not.toBeNull();
    return row!;
  }

  function rows(el: HTMLElement): string[] {
    return Array.from(el.querySelectorAll('table tbody tr')).map((row) =>
      (row.textContent ?? '').replace(/\s+/g, ' ').trim(),
    );
  }

  beforeEach(() => {
    get = vi.fn();
    updateBusinessContext = vi.fn();
    addLink = vi.fn();
    removeLink = vi.fn();
    listFeatures = vi.fn().mockResolvedValue([]);
    listMilestones = vi.fn().mockResolvedValue([]);
  });

  it('bindet den Abschnitt «Milestones» des geöffneten PI auf der Detailseite ein (US-15)', async () => {
    get.mockResolvedValue({ ...PI_27_1, id: '42' });
    listMilestones.mockResolvedValue([{ id: 'm1', name: 'Release 4.0', date: '2027-02-10' }]);

    const el = await open('/program-increments/42');
    // Der Abschnitt entsteht erst mit dem geladenen PI und lädt dann selbst; das braucht eine zweite Runde.
    await settle();

    const section = el.querySelector('app-milestones');
    expect(section, 'Abschnitt app-milestones').not.toBeNull();
    const headings = Array.from(section!.querySelectorAll('h2')).map((h) =>
      (h.textContent ?? '').trim(),
    );
    expect(headings).toContain('Milestones');
    expect(listMilestones).toHaveBeenCalledWith('42');
    expect(section!.textContent).toContain('Release 4.0');
    expect(section!.textContent).toContain('10.02.2027');
  });

  it('zeigt das Feature-Backlog des geöffneten PI (US-9)', async () => {
    get.mockResolvedValue({ ...PI_27_1, id: '42' });

    const el = await open('/program-increments/42');

    expect(el.querySelector('app-feature-backlog')).not.toBeNull();
    expect(listFeatures).toHaveBeenCalledWith('42');
  });

  it('verlinkt auf das Program Board des geöffneten PI (US-12)', async () => {
    get.mockResolvedValue({ ...PI_27_1, id: '42' });

    const el = await open('/program-increments/42');

    expect(
      el.querySelector('a[href="/program-increments/42/board"]'),
      'Link zum Board',
    ).not.toBeNull();
  });

  it('lädt das PI mit der ID aus der URL (US-5)', async () => {
    get.mockResolvedValue({ ...PI_27_1, id: '42' });

    await open('/program-increments/42');

    expect(get).toHaveBeenCalledWith('42');
  });

  it('zeigt die Iterationen mit markierter IP-Iteration (US-5)', async () => {
    get.mockResolvedValue(PI_27_1);

    const lines = rows(await open('/program-increments/1'));

    expect(lines).toHaveLength(6);
    const expected = [
      ['Iteration 1', '04.01.2027', '17.01.2027'],
      ['Iteration 2', '18.01.2027', '31.01.2027'],
      ['Iteration 3', '01.02.2027', '14.02.2027'],
      ['Iteration 4', '15.02.2027', '28.02.2027'],
      ['Iteration 5', '01.03.2027', '14.03.2027'],
      ['IP', '15.03.2027', '26.03.2027'],
    ];
    expected.forEach(([name, start, end], i) => {
      expect(lines[i]).toContain(name);
      expect(lines[i]).toContain(start);
      expect(lines[i]).toContain(end);
    });
    expect(lines[5]).toContain('IP-Iteration');
    expect(lines.slice(0, 5).filter((line) => line.includes('IP-Iteration'))).toEqual([]);
  });

  it('bindet die Kapazitätstabelle des PI auf der Detailseite ein (US-7)', async () => {
    get.mockResolvedValue(PI_27_1);

    const el = await open('/program-increments/1');

    expect(el.querySelector('app-capacity-table'), 'Kapazitätstabelle').not.toBeNull();
  });

  it('zeigt die Meldung, wenn es das PI nicht gibt (US-5)', async () => {
    get.mockRejectedValue(
      new HttpErrorResponse({ status: 404, error: { message: 'Das PI gibt es nicht.' } }),
    );

    const el = await open('/program-increments/unbekannt');

    expect(el.querySelector('[role="alert"]')?.textContent ?? '').toContain(
      'Das PI gibt es nicht.',
    );
    expect(rows(el)).toEqual([]);
  });

  it('zeigt Business-Kontext und PI Objectives mit Zeilenumbrüchen (US-8)', async () => {
    get.mockResolvedValue({
      ...PI_27_1,
      businessContext: 'Fokus auf Self-Service\nNeue Kundengruppe',
      artObjectives: 'Checkout in 2 Schritten\n\nLadezeit unter 1 s',
    });

    const el = await open('/program-increments/1');

    const context = shown(el, 'business-context');
    const objectives = shown(el, 'art-objectives');
    expect(context?.textContent?.trim()).toBe('Fokus auf Self-Service\nNeue Kundengruppe');
    expect(objectives?.textContent?.trim()).toBe('Checkout in 2 Schritten\n\nLadezeit unter 1 s');
    // Ohne pre-line fasst der Browser Zeilenumbrüche zu Leerzeichen zusammen.
    expect(context?.classList.contains('whitespace-pre-line')).toBe(true);
    expect(objectives?.classList.contains('whitespace-pre-line')).toBe(true);
    expect(el.textContent).not.toContain(EMPTY_HINT);
  });

  it('zeigt Text als Text, nicht als HTML (US-8)', async () => {
    get.mockResolvedValue({ ...PI_27_1, businessContext: '<b>fett</b>' });

    const el = await open('/program-increments/1');

    expect(shown(el, 'business-context')?.textContent?.trim()).toBe('<b>fett</b>');
    expect(shown(el, 'business-context')?.querySelector('b')).toBeNull();
  });

  it('zeigt die Links, die sich in einem neuen Tab öffnen (US-8)', async () => {
    get.mockResolvedValue({ ...PI_27_1, links: [VISION, ROADMAP] });

    const el = await open('/program-increments/1');

    const anchors = [VISION, ROADMAP].map((link) => el.querySelector(`a[href="${link.url}"]`));
    expect(anchors.map((a) => a?.textContent?.trim())).toEqual(['Vision', 'Roadmap']);
    for (const a of anchors) {
      expect(a?.getAttribute('target')).toBe('_blank');
      const rel = (a?.getAttribute('rel') ?? '').split(/\s+/);
      expect(rel).toContain('noopener');
      expect(rel).toContain('noreferrer');
    }
  });

  it('zeigt ohne Inhalt den Hinweis, dass noch kein Business-Kontext erfasst ist (US-8)', async () => {
    get.mockResolvedValue(PI_27_1);

    const el = await open('/program-increments/1');

    expect(el.textContent).toContain(EMPTY_HINT);
  });

  it('zeigt den Hinweis nicht, wenn nur ein Link erfasst ist (US-8)', async () => {
    get.mockResolvedValue({ ...PI_27_1, links: [VISION] });

    const el = await open('/program-increments/1');

    expect(el.querySelector(`a[href="${VISION.url}"]`)?.textContent?.trim()).toBe('Vision');
    expect(el.textContent).not.toContain(EMPTY_HINT);
  });

  it('zeigt den Hinweis nicht, wenn nur PI Objectives erfasst sind (US-8)', async () => {
    get.mockResolvedValue({ ...PI_27_1, artObjectives: 'Checkout in 2 Schritten' });

    const el = await open('/program-increments/1');

    expect(shown(el, 'art-objectives')?.textContent?.trim()).toBe('Checkout in 2 Schritten');
    expect(el.textContent).not.toContain(EMPTY_HINT);
  });

  it('zeigt den Knopf «Bearbeiten», wenn nur ein Link erfasst ist (US-8)', async () => {
    get.mockResolvedValue({ ...PI_27_1, links: [VISION] });

    const section = contextSection(await open('/program-increments/1'));

    expect(button(section, 'Bearbeiten'), 'Knopf «Bearbeiten»').toBeDefined();
    expect(button(section, 'Erfassen'), 'Knopf «Erfassen»').toBeUndefined();
  });

  it('zeigt den Knopf «Bearbeiten», wenn nur PI Objectives erfasst sind (US-8)', async () => {
    get.mockResolvedValue({ ...PI_27_1, artObjectives: 'Checkout in 2 Schritten' });

    const section = contextSection(await open('/program-increments/1'));

    expect(button(section, 'Bearbeiten'), 'Knopf «Bearbeiten»').toBeDefined();
    expect(button(section, 'Erfassen'), 'Knopf «Erfassen»').toBeUndefined();
  });

  it('lässt das Formular mit der Eingabe offen, wenn der Server das Speichern ablehnt (US-8)', async () => {
    get.mockResolvedValue({ ...PI_27_1, businessContext: 'Digitalisierung' });
    updateBusinessContext.mockRejectedValue(
      new HttpErrorResponse({ status: 400, error: { message: 'Abgelehnt vom Server.' } }),
    );
    const el = await open('/program-increments/1');
    await openContextForm(el, 'Bearbeiten');
    type(textarea(el, 'businessContext'), 'Self-Service-Portal');

    await submit(textarea(el, 'businessContext'));

    expect(el.querySelector('[role="alert"]')?.textContent ?? '').toContain(
      'Abgelehnt vom Server.',
    );
    expect(textarea(el, 'businessContext').value).toBe('Self-Service-Portal');
    expect(shown(el, 'business-context')?.textContent?.trim()).toBe('Digitalisierung');
  });

  it('ändert den Business-Kontext über «Bearbeiten» (US-8)', async () => {
    get.mockResolvedValue({ ...PI_27_1, businessContext: 'Digitalisierung' });
    updateBusinessContext.mockResolvedValue({ ...PI_27_1, businessContext: 'Self-Service-Portal' });
    const el = await open('/program-increments/1');

    expect(occurrences(el.textContent ?? '', 'Digitalisierung')).toBe(1);
    expect(textareas(el)).toEqual([]);

    await press(el, 'Bearbeiten');
    const context = textarea(el, 'businessContext');
    expect(context.value).toBe('Digitalisierung');
    type(context, 'Self-Service-Portal');
    await submit(context);

    expect(updateBusinessContext).toHaveBeenCalledWith('1', {
      businessContext: 'Self-Service-Portal',
      artObjectives: '',
    });
    expect(shown(el, 'business-context')?.textContent?.trim()).toBe('Self-Service-Portal');
    expect(el.textContent).not.toContain('Digitalisierung');
    expect(textareas(el)).toEqual([]);
  });

  it('zeigt mit Inhalt den Knopf «Bearbeiten», nicht «Erfassen» (US-8)', async () => {
    get.mockResolvedValue({ ...PI_27_1, businessContext: 'Digitalisierung' });

    const section = contextSection(await open('/program-increments/1'));

    expect(button(section, 'Bearbeiten'), 'Knopf «Bearbeiten»').toBeDefined();
    expect(button(section, 'Erfassen'), 'Knopf «Erfassen»').toBeUndefined();
  });

  it('zeigt ohne Inhalt den Hinweis mit dem Knopf «Erfassen», der ein leeres Formular öffnet (US-8)', async () => {
    get.mockResolvedValue(PI_27_1);
    const el = await open('/program-increments/1');
    const section = contextSection(el);

    expect(section.textContent).toContain(EMPTY_HINT);
    expect(button(section, 'Bearbeiten'), 'Knopf «Bearbeiten»').toBeUndefined();
    expect(textareas(el)).toEqual([]);

    await press(el, 'Erfassen');

    expect(textarea(el, 'businessContext').value).toBe('');
    expect(textarea(el, 'artObjectives').value).toBe('');
  });

  it('schliesst beim Abbrechen das Formular und verwirft die Eingabe (US-8)', async () => {
    get.mockResolvedValue({
      ...PI_27_1,
      businessContext: 'Digitalisierung',
      artObjectives: 'Checkout in 2 Schritten',
    });
    const el = await open('/program-increments/1');
    await openContextForm(el, 'Bearbeiten');
    type(textarea(el, 'businessContext'), 'Verworfen');
    type(textarea(el, 'artObjectives'), 'Auch verworfen');

    await press(el, 'Abbrechen');

    expect(updateBusinessContext).not.toHaveBeenCalled();
    expect(textareas(el)).toEqual([]);
    expect(shown(el, 'business-context')?.textContent?.trim()).toBe('Digitalisierung');
    expect(el.textContent).not.toContain('Verworfen');
    await openContextForm(el, 'Bearbeiten');
    expect(textarea(el, 'businessContext').value).toBe('Digitalisierung');
    expect(textarea(el, 'artObjectives').value).toBe('Checkout in 2 Schritten');
  });

  it('zeigt das Link-Formular erst nach «Link hinzufügen» (US-8)', async () => {
    get.mockResolvedValue({ ...PI_27_1, businessContext: 'Digitalisierung' });
    const el = await open('/program-increments/1');

    expect(contextSection(el).querySelector('input'), 'Link-Formular').toBeNull();

    await press(el, 'Link hinzufügen');

    expect(linkInput(el, 'title').value).toBe('');
    expect(linkInput(el, 'url').value).toBe('');
  });

  it('schliesst beim Abbrechen das Link-Formular und verwirft die Eingabe (US-8)', async () => {
    get.mockResolvedValue(PI_27_1);
    const el = await open('/program-increments/1');
    await openLinkForm(el);
    type(linkInput(el, 'title'), 'Vision');
    type(linkInput(el, 'url'), 'https://wiki.example.ch/vision');

    await press(el, 'Abbrechen');

    expect(addLink).not.toHaveBeenCalled();
    expect(contextSection(el).querySelector('input'), 'Link-Formular').toBeNull();
    await openLinkForm(el);
    expect(linkInput(el, 'title').value).toBe('');
    expect(linkInput(el, 'url').value).toBe('');
  });

  it('füllt die Eingabefelder mit den erfassten Texten (US-8)', async () => {
    get.mockResolvedValue({
      ...PI_27_1,
      businessContext: 'Fokus auf Self-Service',
      artObjectives: 'Checkout in 2 Schritten',
    });
    const el = await open('/program-increments/1');
    await openContextForm(el, 'Bearbeiten');

    expect(textarea(el, 'businessContext').value).toBe('Fokus auf Self-Service');
    expect(textarea(el, 'artObjectives').value).toBe('Checkout in 2 Schritten');
  });

  it('erfasst Business-Kontext und PI Objectives und zeigt das gespeicherte PI (US-8)', async () => {
    get.mockResolvedValue(PI_27_1);
    updateBusinessContext.mockResolvedValue({
      ...PI_27_1,
      businessContext: 'Fokus auf Self-Service\nNeue Kundengruppe',
      artObjectives: 'Checkout in 2 Schritten',
    });
    const el = await open('/program-increments/1');
    await openContextForm(el, 'Erfassen');
    const context = textarea(el, 'businessContext');
    type(context, 'Fokus auf Self-Service\nNeue Kundengruppe');
    type(textarea(el, 'artObjectives'), 'Checkout in 2 Schritten');

    await submit(context);

    expect(updateBusinessContext).toHaveBeenCalledWith('1', {
      businessContext: 'Fokus auf Self-Service\nNeue Kundengruppe',
      artObjectives: 'Checkout in 2 Schritten',
    });
    expect(shown(el, 'business-context')?.textContent?.trim()).toBe(
      'Fokus auf Self-Service\nNeue Kundengruppe',
    );
    expect(shown(el, 'art-objectives')?.textContent?.trim()).toBe('Checkout in 2 Schritten');
    expect(el.textContent).not.toContain(EMPTY_HINT);
  });

  it('erfasst einen Text von genau 10 000 Zeichen (US-8)', async () => {
    get.mockResolvedValue(PI_27_1);
    updateBusinessContext.mockResolvedValue(PI_27_1);
    const el = await open('/program-increments/1');
    await openContextForm(el, 'Erfassen');
    const context = textarea(el, 'businessContext');
    type(context, 'x'.repeat(10_000));

    await submit(context);

    expect(updateBusinessContext).toHaveBeenCalledWith('1', {
      businessContext: 'x'.repeat(10_000),
      artObjectives: '',
    });
  });

  it('zeigt einen zu langen Text am Feld an, ohne zu speichern (US-8)', async () => {
    get.mockResolvedValue(PI_27_1);
    const el = await open('/program-increments/1');
    await openContextForm(el, 'Erfassen');
    const objectives = textarea(el, 'artObjectives');
    type(objectives, 'x'.repeat(10_001));

    await submit(objectives);

    expect(objectives.closest('label')?.textContent ?? '').toContain(
      'Der Text darf höchstens 10 000 Zeichen lang sein.',
    );
    expect(updateBusinessContext).not.toHaveBeenCalled();
  });

  it('fügt einen Link hinzu, zeigt ihn und leert das Formular (US-8)', async () => {
    get.mockResolvedValue(PI_27_1);
    addLink.mockResolvedValue({ ...PI_27_1, links: [VISION] });
    const el = await open('/program-increments/1');
    await openLinkForm(el);
    type(linkInput(el, 'title'), 'Vision');
    type(linkInput(el, 'url'), 'https://wiki.example.ch/vision');

    await submit(linkInput(el, 'title'));

    expect(addLink).toHaveBeenCalledWith('1', {
      title: 'Vision',
      url: 'https://wiki.example.ch/vision',
    });
    expect(el.querySelector(`a[href="${VISION.url}"]`)?.textContent?.trim()).toBe('Vision');
    // Nach dem Hinzufügen ist das Link-Formular zu; geöffnet ist es wieder leer.
    await openLinkForm(el);
    expect(linkInput(el, 'title').value).toBe('');
    expect(linkInput(el, 'url').value).toBe('');
  });

  it('behält ungespeicherten Business-Kontext, wenn ein Link hinzukommt (US-8)', async () => {
    get.mockResolvedValue(PI_27_1);
    addLink.mockResolvedValue({ ...PI_27_1, links: [VISION] });
    const el = await open('/program-increments/1');
    await openContextForm(el, 'Erfassen');
    type(textarea(el, 'businessContext'), 'Noch nicht gespeichert');
    await press(el, 'Link hinzufügen');
    type(linkInput(el, 'title'), 'Vision');
    type(linkInput(el, 'url'), 'https://wiki.example.ch/vision');

    await submit(linkInput(el, 'title'));

    expect(textarea(el, 'businessContext').value).toBe('Noch nicht gespeichert');
  });

  it('fügt ohne Titel keinen Link hinzu (US-8)', async () => {
    get.mockResolvedValue(PI_27_1);
    const el = await open('/program-increments/1');
    await openLinkForm(el);
    type(linkInput(el, 'url'), 'https://wiki.example.ch/vision');

    await submit(linkInput(el, 'url'));

    expect(addLink).not.toHaveBeenCalled();
  });

  it('fügt ohne URL keinen Link hinzu (US-8)', async () => {
    get.mockResolvedValue(PI_27_1);
    const el = await open('/program-increments/1');
    await openLinkForm(el);
    type(linkInput(el, 'title'), 'Vision');

    await submit(linkInput(el, 'title'));

    expect(addLink).not.toHaveBeenCalled();
  });

  it('zeigt die Meldung des Servers, wenn ein Link abgelehnt wird, und behält die Eingabe (US-8)', async () => {
    get.mockResolvedValue(PI_27_1);
    addLink.mockRejectedValue(
      new HttpErrorResponse({
        status: 400,
        error: { message: 'Die URL muss mit http:// oder https:// beginnen.' },
      }),
    );
    const el = await open('/program-increments/1');
    await openLinkForm(el);
    type(linkInput(el, 'title'), 'Vision');
    type(linkInput(el, 'url'), 'javascript:alert(1)');

    await submit(linkInput(el, 'title'));

    expect(el.querySelector('[role="alert"]')?.textContent ?? '').toContain(
      'Die URL muss mit http:// oder https:// beginnen.',
    );
    expect(el.querySelector('h1')?.textContent).toContain('PI 27.1');
    expect(linkInput(el, 'url').value).toBe('javascript:alert(1)');
  });

  it('entfernt genau den Link, dessen Knopf gedrückt wird (US-8)', async () => {
    get.mockResolvedValue({ ...PI_27_1, links: [VISION, ROADMAP] });
    removeLink.mockResolvedValue({ ...PI_27_1, links: [VISION] });
    const el = await open('/program-increments/1');
    const roadmap = el.querySelector(`a[href="${ROADMAP.url}"]`);
    expect(roadmap, 'Link «Roadmap»').not.toBeNull();
    const remove = button(linkRow(roadmap!), 'Entfernen');
    expect(remove, 'Knopf «Entfernen» beim Link').toBeDefined();

    remove!.click();
    await settle();

    expect(removeLink).toHaveBeenCalledWith('1', 'l2');
    expect(el.querySelector(`a[href="${ROADMAP.url}"]`)).toBeNull();
    expect(el.querySelector(`a[href="${VISION.url}"]`)).not.toBeNull();
  });
});
