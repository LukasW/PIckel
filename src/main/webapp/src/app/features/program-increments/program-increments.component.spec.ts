import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ProgramIncrementsRepository } from '../../core/ports/program-increments.repository';
import { ProgramIncrementsComponent } from './program-increments.component';

/** Iterationen von PI 27.1 (04.01.–26.03.2027) zu 2 Wochen, wie sie der Server liefert (US-5). */
const ITERATIONS_27_1 = [
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
}));

const PI_27_1 = {
  id: '1',
  name: 'PI 27.1',
  startDate: '2027-01-04',
  endDate: '2027-03-26',
  iterationLengthWeeks: 2,
  iterations: ITERATIONS_27_1,
};

describe('ProgramIncrementsComponent', () => {
  let plan: ReturnType<typeof vi.fn>;

  async function render(list: unknown[]) {
    plan = vi.fn();
    TestBed.configureTestingModule({
      imports: [ProgramIncrementsComponent],
      providers: [
        provideRouter([]),
        {
          provide: ProgramIncrementsRepository,
          useValue: { list: vi.fn().mockResolvedValue(list), plan },
        },
      ],
    });
    const fixture = TestBed.createComponent(ProgramIncrementsComponent);
    await fixture.whenStable();
    return fixture;
  }

  function input(el: HTMLElement, name: string): HTMLInputElement {
    return el.querySelector(`input[formControlName="${name}"]`) as HTMLInputElement;
  }

  async function submit(fixture: Awaited<ReturnType<typeof render>>) {
    const el = fixture.nativeElement as HTMLElement;
    el.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    return el;
  }

  function iterationLength(el: HTMLElement): HTMLSelectElement {
    const select = el.querySelector('select[formControlName="iterationLengthWeeks"]');
    expect(select, 'Auswahl der Iterationslänge').not.toBeNull();
    return select as HTMLSelectElement;
  }

  /** Beschriftungen der Auswahl, bei «2 Wochen» also «2 Wochen». */
  function optionLabels(select: HTMLSelectElement): string[] {
    return Array.from(select.options).map((option) => option.textContent!.trim());
  }

  /** Wählt die Option, deren Beschriftung mit der Zahl beginnt, wie es ein Nutzer täte. */
  function chooseWeeks(el: HTMLElement, weeks: number) {
    const select = iterationLength(el);
    select.selectedIndex = optionLabels(select).findIndex((label) =>
      new RegExp(`^${weeks}\\b`).test(label),
    );
    select.dispatchEvent(new Event('change'));
  }

  /** Text des Feldbereichs (label) eines Eingabefelds, damit eine Meldung «am Feld» belegt ist. */
  function field(el: HTMLElement, name: string): string {
    return input(el, name).closest('label')!.textContent ?? '';
  }

  function fill(el: HTMLElement, name: string) {
    for (const [control, value] of [
      ['name', name],
      ['startDate', '2027-01-04'],
      ['endDate', '2027-03-26'],
    ]) {
      input(el, control).value = value;
      input(el, control).dispatchEvent(new Event('input'));
    }
  }

  it('zeigt einen Leerzustand ohne PIs (US-1)', async () => {
    const el = (await render([])).nativeElement as HTMLElement;

    expect(el.textContent).toContain('Noch keine PIs');
  });

  it('listet vorhandene PIs (US-1)', async () => {
    const el = (await render([PI_27_1])).nativeElement as HTMLElement;

    const rows = el.querySelectorAll('[data-testid="pi-row"]');
    expect(rows).toHaveLength(1);
    expect(rows[0].textContent).toContain('PI 27.1');
  });

  it('zeigt Daten im Format TT.MM.JJJJ (US-1)', async () => {
    const el = (await render([PI_27_1])).nativeElement as HTMLElement;

    expect(el.querySelector('[data-testid="pi-row"]')!.textContent).toContain(
      '04.01.2027 – 26.03.2027',
    );
  });

  it('zeigt Pflichtfelder am Feld an, ohne anzulegen (US-1)', async () => {
    const el = await submit(await render([]));

    expect(field(el, 'name')).toContain('Der Name ist Pflicht.');
    expect(field(el, 'startDate')).toContain('Das Startdatum ist Pflicht.');
    expect(field(el, 'endDate')).toContain('Das Enddatum ist Pflicht.');
    expect(input(el, 'name').getAttribute('aria-invalid')).toBe('true');
    expect(plan).not.toHaveBeenCalled();
  });

  it('lehnt einen Namen aus Leerzeichen im Formular ab (US-1)', async () => {
    const fixture = await render([]);
    const el = fixture.nativeElement as HTMLElement;
    for (const [name, value] of [
      ['name', '   '],
      ['startDate', '2027-01-04'],
      ['endDate', '2027-03-26'],
    ]) {
      input(el, name).value = value;
      input(el, name).dispatchEvent(new Event('input'));
    }

    await submit(fixture);

    expect(el.textContent).toContain('Der Name ist Pflicht.');
    expect(plan).not.toHaveBeenCalled();
  });

  it('legt mit gültigen Angaben an und leert danach das Formular (US-1)', async () => {
    const fixture = await render([]);
    const el = fixture.nativeElement as HTMLElement;
    const name = 'x'.repeat(100);
    fill(el, name);
    plan.mockResolvedValue({ ...PI_27_1, name });

    await submit(fixture);

    expect(plan).toHaveBeenCalledWith({
      name,
      startDate: '2027-01-04',
      endDate: '2027-03-26',
      iterationLengthWeeks: 2,
    });
    expect(input(el, 'name').value).toBe('');
    expect(field(el, 'name')).not.toContain('Der Name ist Pflicht.');
  });

  it('misst die Länge des Namens wie der Server ohne Leerzeichen am Rand (US-1)', async () => {
    const fixture = await render([]);
    const el = fixture.nativeElement as HTMLElement;
    fill(el, '  ' + 'x'.repeat(100) + '  ');

    await submit(fixture);

    expect(field(el, 'name')).not.toContain('höchstens');
    expect(plan).toHaveBeenCalled();
  });

  it('zeigt einen zu langen Namen am Feld an (US-1)', async () => {
    const fixture = await render([]);
    const el = fixture.nativeElement as HTMLElement;
    input(el, 'name').value = 'x'.repeat(101);
    input(el, 'name').dispatchEvent(new Event('input'));

    await submit(fixture);

    expect(field(el, 'name')).toContain('Der Name darf höchstens 100 Zeichen lang sein.');
    expect(plan).not.toHaveBeenCalled();
  });

  it('bietet Iterationslängen von 1 bis 4 Wochen an, vorgewählt 2 Wochen (US-5)', async () => {
    const el = (await render([])).nativeElement as HTMLElement;
    const select = iterationLength(el);

    expect(optionLabels(select).map((label) => label.match(/^\d+/)?.[0])).toEqual([
      '1',
      '2',
      '3',
      '4',
    ]);
    expect(optionLabels(select)[select.selectedIndex]).toMatch(/^2\b/);
  });

  it('legt ohne Auswahl mit Iterationen zu 2 Wochen an (US-5)', async () => {
    const fixture = await render([]);
    const el = fixture.nativeElement as HTMLElement;
    fill(el, 'PI 27.1');
    plan.mockResolvedValue(PI_27_1);

    await submit(fixture);

    expect(plan).toHaveBeenCalledWith({
      name: 'PI 27.1',
      startDate: '2027-01-04',
      endDate: '2027-03-26',
      iterationLengthWeeks: 2,
    });
  });

  it('legt nach Wahl von 3 Wochen mit Iterationen zu 3 Wochen an (US-5)', async () => {
    const fixture = await render([]);
    const el = fixture.nativeElement as HTMLElement;
    fill(el, 'PI 27.1');
    chooseWeeks(el, 3);
    plan.mockResolvedValue({ ...PI_27_1, iterationLengthWeeks: 3 });

    await submit(fixture);

    expect(plan).toHaveBeenCalledWith({
      name: 'PI 27.1',
      startDate: '2027-01-04',
      endDate: '2027-03-26',
      iterationLengthWeeks: 3,
    });
  });

  it('setzt die Iterationslänge nach dem Anlegen auf 2 Wochen zurück (US-5)', async () => {
    const fixture = await render([]);
    const el = fixture.nativeElement as HTMLElement;
    fill(el, 'PI 27.1');
    chooseWeeks(el, 3);
    plan.mockResolvedValue({ ...PI_27_1, iterationLengthWeeks: 3 });

    await submit(fixture);

    const select = iterationLength(el);
    expect(optionLabels(select)[select.selectedIndex]).toMatch(/^2\b/);
  });

  it('verlinkt jedes PI der Liste auf seine Detailseite (US-5)', async () => {
    const el = (
      await render([
        PI_27_1,
        { ...PI_27_1, id: '2', name: 'PI 27.2', startDate: '2027-03-29', endDate: '2027-06-18' },
      ])
    ).nativeElement as HTMLElement;

    const links = Array.from(el.querySelectorAll('[data-testid="pi-row"]')).map((row) =>
      row.querySelector('a')?.getAttribute('href'),
    );
    expect(links).toEqual(['/program-increments/1', '/program-increments/2']);
  });
});
