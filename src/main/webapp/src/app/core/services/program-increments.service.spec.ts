import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ProgramIncrement } from '../models/program-increment.model';
import { ProgramIncrementsRepository } from '../ports/program-increments.repository';
import { ProgramIncrementsService } from './program-increments.service';

const PI_27_2: ProgramIncrement = {
  id: '2',
  name: 'PI 27.2',
  startDate: '2027-03-29',
  endDate: '2027-06-18',
  iterationLengthWeeks: 3,
  iterations: [
    {
      number: 1,
      name: 'Iteration 1',
      startDate: '2027-03-29',
      endDate: '2027-04-18',
      innovationAndPlanning: false,
    },
    {
      number: 2,
      name: 'Iteration 2',
      startDate: '2027-04-19',
      endDate: '2027-05-09',
      innovationAndPlanning: false,
    },
    {
      number: 3,
      name: 'Iteration 3',
      startDate: '2027-05-10',
      endDate: '2027-05-30',
      innovationAndPlanning: false,
    },
    {
      number: 4,
      name: 'IP',
      startDate: '2027-05-31',
      endDate: '2027-06-18',
      innovationAndPlanning: true,
    },
  ],
};
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

describe('ProgramIncrementsService', () => {
  let repository: {
    list: ReturnType<typeof vi.fn>;
    get: ReturnType<typeof vi.fn>;
    plan: ReturnType<typeof vi.fn>;
  };
  let service: ProgramIncrementsService;

  beforeEach(() => {
    repository = { list: vi.fn(), get: vi.fn(), plan: vi.fn() };
    TestBed.configureTestingModule({
      providers: [{ provide: ProgramIncrementsRepository, useValue: repository }],
    });
    service = TestBed.inject(ProgramIncrementsService);
  });

  it('übernimmt die Reihenfolge des Servers, der nach Startdatum sortiert (US-1)', async () => {
    repository.list.mockResolvedValue([PI_27_1, PI_27_2]);

    await service.load();

    expect(service.programIncrements().map((pi) => pi.name)).toEqual(['PI 27.1', 'PI 27.2']);
    expect(service.loading()).toBe(false);
  });

  it('lädt nach dem Anlegen die Liste in der Reihenfolge des Servers neu (US-1)', async () => {
    repository.list.mockResolvedValueOnce([PI_27_2]).mockResolvedValueOnce([PI_27_1, PI_27_2]);
    repository.plan.mockResolvedValue(PI_27_1);
    await service.load();

    const ok = await service.plan({
      name: 'PI 27.1',
      startDate: '2027-01-04',
      endDate: '2027-03-26',
    });

    expect(ok).toBe(true);
    expect(repository.list).toHaveBeenCalledTimes(2);
    expect(service.programIncrements()).toEqual([PI_27_1, PI_27_2]);
  });

  it('zeigt die Meldung des Servers, wenn das Anlegen abgelehnt wird (US-1)', async () => {
    repository.plan.mockRejectedValue(
      new HttpErrorResponse({
        status: 400,
        error: { message: 'Das Enddatum muss nach dem Startdatum liegen.' },
      }),
    );

    const ok = await service.plan({
      name: 'PI 27.1',
      startDate: '2027-03-26',
      endDate: '2027-01-04',
    });

    expect(ok).toBe(false);
    expect(service.error()).toBe('Das Enddatum muss nach dem Startdatum liegen.');
    expect(repository.list).not.toHaveBeenCalled();
  });

  it('lädt ein PI mit seinen Iterationen über die ID (US-5)', async () => {
    repository.get.mockResolvedValue(PI_27_1);

    const pi = await service.get('1');

    expect(repository.get).toHaveBeenCalledWith('1');
    expect(pi).toEqual(PI_27_1);
    expect(service.error()).toBeUndefined();
  });

  it('liefert kein PI und zeigt die Meldung des Servers, wenn es das PI nicht gibt (US-5)', async () => {
    repository.get.mockRejectedValue(
      new HttpErrorResponse({ status: 404, error: { message: 'Das PI gibt es nicht.' } }),
    );

    const pi = await service.get('unbekannt');

    expect(pi).toBeUndefined();
    expect(service.error()).toBe('Das PI gibt es nicht.');
  });

  it('vergisst die Meldung eines nicht gefundenen PI, sobald ein PI gefunden wird (US-5)', async () => {
    repository.get
      .mockRejectedValueOnce(
        new HttpErrorResponse({ status: 404, error: { message: 'Das PI gibt es nicht.' } }),
      )
      .mockResolvedValueOnce(PI_27_1);
    await service.get('unbekannt');

    const pi = await service.get('1');

    expect(pi).toEqual(PI_27_1);
    expect(service.error()).toBeUndefined();
  });
});
