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
};
const PI_27_1: ProgramIncrement = {
  id: '1',
  name: 'PI 27.1',
  startDate: '2027-01-04',
  endDate: '2027-03-26',
};

describe('ProgramIncrementsService', () => {
  let repository: { list: ReturnType<typeof vi.fn>; plan: ReturnType<typeof vi.fn> };
  let service: ProgramIncrementsService;

  beforeEach(() => {
    repository = { list: vi.fn(), plan: vi.fn() };
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
});
