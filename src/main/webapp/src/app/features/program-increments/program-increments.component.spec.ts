import { TestBed } from '@angular/core/testing';
import { ProgramIncrementsRepository } from '../../core/ports/program-increments.repository';
import { ProgramIncrementsComponent } from './program-increments.component';

describe('ProgramIncrementsComponent', () => {
  let plan: ReturnType<typeof vi.fn>;

  async function render(list: unknown[]) {
    plan = vi.fn();
    TestBed.configureTestingModule({
      imports: [ProgramIncrementsComponent],
      providers: [
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
    const el = (
      await render([{ id: '1', name: 'PI 27.1', startDate: '2027-01-04', endDate: '2027-03-26' }])
    ).nativeElement as HTMLElement;

    const rows = el.querySelectorAll('[data-testid="pi-row"]');
    expect(rows).toHaveLength(1);
    expect(rows[0].textContent).toContain('PI 27.1');
  });

  it('zeigt Daten im Format TT.MM.JJJJ (US-1)', async () => {
    const el = (
      await render([{ id: '1', name: 'PI 27.1', startDate: '2027-01-04', endDate: '2027-03-26' }])
    ).nativeElement as HTMLElement;

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
    plan.mockResolvedValue({ id: '1', name, startDate: '2027-01-04', endDate: '2027-03-26' });

    await submit(fixture);

    expect(plan).toHaveBeenCalledWith({ name, startDate: '2027-01-04', endDate: '2027-03-26' });
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
});
