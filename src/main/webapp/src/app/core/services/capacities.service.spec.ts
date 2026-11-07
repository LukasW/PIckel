import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Capacity } from '../models/capacity.model';
import { CapacitiesRepository } from '../ports/capacities.repository';
import { CapacitiesService } from './capacities.service';

const GENERIC_ERROR = 'Das hat nicht geklappt. Bitte versuche es nochmals.';
const NEGATIVE_REJECTED = 'Die Kapazität muss eine ganze Zahl ab 0 sein.';
const RAKETE_1: Capacity = { teamId: 'r', iterationNumber: 1, points: 40 };
const RAKETE_2: Capacity = { teamId: 'r', iterationNumber: 2, points: 0 };
const RAKETE_2_SET: Capacity = { teamId: 'r', iterationNumber: 2, points: 35 };

describe('CapacitiesService', () => {
  let repository: {
    list: ReturnType<typeof vi.fn>;
    set: ReturnType<typeof vi.fn>;
  };
  let service: CapacitiesService;

  beforeEach(() => {
    repository = { list: vi.fn(), set: vi.fn() };
    TestBed.configureTestingModule({
      providers: [{ provide: CapacitiesRepository, useValue: repository }],
    });
    service = TestBed.inject(CapacitiesService);
  });

  it('lädt die Kapazitäten des gewählten PI (US-7)', async () => {
    repository.list.mockResolvedValue([RAKETE_1, RAKETE_2]);

    await service.load('pi-2');

    expect(repository.list).toHaveBeenCalledWith('pi-2');
    expect(service.capacities()).toEqual([RAKETE_1, RAKETE_2]);
    expect(service.error()).toBeUndefined();
  });

  it('zeigt die allgemeine Meldung, wenn das Laden scheitert (US-7)', async () => {
    repository.list.mockRejectedValue(new Error('offline'));

    await service.load('pi-1');

    expect(service.error()).toBe(GENERIC_ERROR);
  });

  it('erfasst die Kapazität einer Zelle und zeigt den neuen Wert (US-7)', async () => {
    // Vor dem Erfassen liefert der Server 0, danach 35; egal ob der Service neu lädt oder die Zelle ersetzt.
    repository.list
      .mockResolvedValueOnce([RAKETE_1, RAKETE_2])
      .mockResolvedValue([RAKETE_1, RAKETE_2_SET]);
    repository.set.mockResolvedValue(RAKETE_2_SET);
    await service.load('pi-1');

    const ok = await service.set('pi-1', 'r', 2, 35);

    expect(ok).toBe(true);
    expect(repository.set).toHaveBeenCalledWith('pi-1', 'r', 2, 35);
    expect(service.capacities()).toEqual([RAKETE_1, RAKETE_2_SET]);
    expect(service.error()).toBeUndefined();
  });

  it('zeigt die Meldung des Servers, wenn eine negative Kapazität abgelehnt wird (US-7)', async () => {
    repository.list.mockResolvedValue([RAKETE_1, RAKETE_2]);
    repository.set.mockRejectedValue(
      new HttpErrorResponse({ status: 400, error: { message: NEGATIVE_REJECTED } }),
    );
    await service.load('pi-1');

    const ok = await service.set('pi-1', 'r', 2, -5);

    expect(ok).toBe(false);
    expect(service.error()).toBe(NEGATIVE_REJECTED);
    expect(service.capacities()).toEqual([RAKETE_1, RAKETE_2]);
  });

  it('zeigt die allgemeine Meldung, wenn der Server keine Meldung liefert (US-7)', async () => {
    repository.set.mockRejectedValue(new HttpErrorResponse({ status: 500, error: 'boom' }));

    const ok = await service.set('pi-1', 'r', 1, 10);

    expect(ok).toBe(false);
    expect(service.error()).toBe(GENERIC_ERROR);
  });

  it('vergisst eine alte Meldung, sobald ein Erfassen gelingt (US-7)', async () => {
    repository.list.mockResolvedValue([RAKETE_1, RAKETE_2_SET]);
    repository.set
      .mockRejectedValueOnce(
        new HttpErrorResponse({ status: 400, error: { message: NEGATIVE_REJECTED } }),
      )
      .mockResolvedValueOnce(RAKETE_2_SET);
    await service.set('pi-1', 'r', 2, -5);

    const ok = await service.set('pi-1', 'r', 2, 35);

    expect(ok).toBe(true);
    expect(service.error()).toBeUndefined();
  });
});
