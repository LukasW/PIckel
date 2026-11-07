import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { RouterTestingHarness } from '@angular/router/testing';
import { appConfig } from '../../app.config';
import { ProgramIncrement } from '../../core/models/program-increment.model';
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
};

describe('ProgramIncrementDetailComponent', () => {
  let get: ReturnType<typeof vi.fn>;

  /** Öffnet die Detailseite über die echte Route, damit die ID aus der URL kommt wie in der App. */
  async function open(url: string): Promise<HTMLElement> {
    TestBed.configureTestingModule({
      providers: [
        ...appConfig.providers,
        { provide: ProgramIncrementsRepository, useValue: { list: vi.fn(), get, plan: vi.fn() } },
      ],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url);
    await harness.fixture.whenStable();
    harness.detectChanges();
    return harness.routeNativeElement as HTMLElement;
  }

  function rows(el: HTMLElement): string[] {
    return Array.from(el.querySelectorAll('table tbody tr')).map((row) =>
      (row.textContent ?? '').replace(/\s+/g, ' ').trim(),
    );
  }

  beforeEach(() => {
    get = vi.fn();
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
});
