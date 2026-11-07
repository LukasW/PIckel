import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { RouterTestingHarness } from '@angular/router/testing';
import { appConfig } from '../../app.config';
import { ProgramIncrement } from '../../core/models/program-increment.model';
import { FeaturesRepository } from '../../core/ports/features.repository';
import { ProgramIncrementsRepository } from '../../core/ports/program-increments.repository';

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
   * Eingabefeld des Business-Kontexts; steckt es hinter einem Knopf «Bearbeiten», wird er zuerst gedrückt. Die
   * Spezifikation legt nur fest, dass sich die Texte auf der Detailseite erfassen lassen, nicht ob sofort sichtbar.
   */
  async function textarea(el: HTMLElement, name: string): Promise<HTMLTextAreaElement> {
    const selector = `textarea[formControlName="${name}"]`;
    if (!el.querySelector(selector)) {
      button(el, 'Bearbeiten')?.click();
      await settle();
    }
    const found = el.querySelector(selector);
    expect(found, `Eingabefeld ${name}`).not.toBeNull();
    return found as HTMLTextAreaElement;
  }

  function linkInput(el: HTMLElement, name: string): HTMLInputElement {
    const found = el.querySelector(`input[formControlName="${name}"]`);
    expect(found, `Eingabefeld ${name}`).not.toBeNull();
    return found as HTMLInputElement;
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
  });

  it('zeigt das Feature-Backlog des geöffneten PI (US-9)', async () => {
    get.mockResolvedValue({ ...PI_27_1, id: '42' });

    const el = await open('/program-increments/42');

    expect(el.querySelector('app-feature-backlog')).not.toBeNull();
    expect(listFeatures).toHaveBeenCalledWith('42');
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

  it('füllt die Eingabefelder mit den erfassten Texten (US-8)', async () => {
    get.mockResolvedValue({
      ...PI_27_1,
      businessContext: 'Fokus auf Self-Service',
      artObjectives: 'Checkout in 2 Schritten',
    });
    const el = await open('/program-increments/1');

    expect((await textarea(el, 'businessContext')).value).toBe('Fokus auf Self-Service');
    expect((await textarea(el, 'artObjectives')).value).toBe('Checkout in 2 Schritten');
  });

  it('erfasst Business-Kontext und PI Objectives und zeigt das gespeicherte PI (US-8)', async () => {
    get.mockResolvedValue(PI_27_1);
    updateBusinessContext.mockResolvedValue({
      ...PI_27_1,
      businessContext: 'Fokus auf Self-Service\nNeue Kundengruppe',
      artObjectives: 'Checkout in 2 Schritten',
    });
    const el = await open('/program-increments/1');
    const context = await textarea(el, 'businessContext');
    type(context, 'Fokus auf Self-Service\nNeue Kundengruppe');
    type(await textarea(el, 'artObjectives'), 'Checkout in 2 Schritten');

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
    const context = await textarea(el, 'businessContext');
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
    const objectives = await textarea(el, 'artObjectives');
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
    type(linkInput(el, 'title'), 'Vision');
    type(linkInput(el, 'url'), 'https://wiki.example.ch/vision');

    await submit(linkInput(el, 'title'));

    expect(addLink).toHaveBeenCalledWith('1', {
      title: 'Vision',
      url: 'https://wiki.example.ch/vision',
    });
    expect(el.querySelector(`a[href="${VISION.url}"]`)?.textContent?.trim()).toBe('Vision');
    expect(linkInput(el, 'title').value).toBe('');
    expect(linkInput(el, 'url').value).toBe('');
  });

  it('behält ungespeicherten Business-Kontext, wenn ein Link hinzukommt (US-8)', async () => {
    get.mockResolvedValue(PI_27_1);
    addLink.mockResolvedValue({ ...PI_27_1, links: [VISION] });
    const el = await open('/program-increments/1');
    type(await textarea(el, 'businessContext'), 'Noch nicht gespeichert');
    type(linkInput(el, 'title'), 'Vision');
    type(linkInput(el, 'url'), 'https://wiki.example.ch/vision');

    await submit(linkInput(el, 'title'));

    expect((await textarea(el, 'businessContext')).value).toBe('Noch nicht gespeichert');
  });

  it('fügt ohne Titel keinen Link hinzu (US-8)', async () => {
    get.mockResolvedValue(PI_27_1);
    const el = await open('/program-increments/1');
    type(linkInput(el, 'url'), 'https://wiki.example.ch/vision');

    await submit(linkInput(el, 'url'));

    expect(addLink).not.toHaveBeenCalled();
  });

  it('fügt ohne URL keinen Link hinzu (US-8)', async () => {
    get.mockResolvedValue(PI_27_1);
    const el = await open('/program-increments/1');
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
