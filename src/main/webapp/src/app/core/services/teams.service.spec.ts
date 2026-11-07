import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Team } from '../models/team.model';
import { TeamsRepository } from '../ports/teams.repository';
import { TeamsService } from './teams.service';

const GENERIC_ERROR = 'Das hat nicht geklappt. Bitte versuche es nochmals.';
const ADLER: Team = { id: 'a', name: 'Adler' };
const RAKETE: Team = { id: 'r', name: 'Rakete' };
const KOMET: Team = { id: 'r', name: 'Komet' };
const ZEBRA: Team = { id: 'z', name: 'Zebra' };

describe('TeamsService', () => {
  let repository: {
    list: ReturnType<typeof vi.fn>;
    add: ReturnType<typeof vi.fn>;
    rename: ReturnType<typeof vi.fn>;
    remove: ReturnType<typeof vi.fn>;
  };
  let service: TeamsService;

  beforeEach(() => {
    repository = { list: vi.fn(), add: vi.fn(), rename: vi.fn(), remove: vi.fn() };
    TestBed.configureTestingModule({
      providers: [{ provide: TeamsRepository, useValue: repository }],
    });
    service = TestBed.inject(TeamsService);
  });

  it('übernimmt die alphabetische Reihenfolge des Servers (US-6)', async () => {
    // Bewusst nicht alphabetisch: der Server sortiert (AK 3), der Client darf nicht umsortieren.
    repository.list.mockResolvedValue([ZEBRA, ADLER]);

    await service.load();

    expect(service.teams().map((team) => team.name)).toEqual(['Zebra', 'Adler']);
    expect(service.loading()).toBe(false);
    expect(service.error()).toBeUndefined();
  });

  it('zeigt die allgemeine Meldung, wenn das Laden scheitert (US-6)', async () => {
    repository.list.mockRejectedValue(new Error('offline'));

    await service.load();

    expect(service.error()).toBe(GENERIC_ERROR);
    expect(service.loading()).toBe(false);
  });

  it('lädt nach dem Erfassen die Liste in der Reihenfolge des Servers neu (US-6)', async () => {
    repository.list.mockResolvedValueOnce([RAKETE]).mockResolvedValueOnce([ADLER, RAKETE]);
    repository.add.mockResolvedValue(ADLER);
    await service.load();

    const ok = await service.add('Adler');

    expect(ok).toBe(true);
    expect(repository.add).toHaveBeenCalledWith('Adler');
    expect(repository.list).toHaveBeenCalledTimes(2);
    expect(service.teams()).toEqual([ADLER, RAKETE]);
  });

  it('zeigt die Meldung des Servers, wenn ein doppelter Name abgelehnt wird (US-6)', async () => {
    repository.add.mockRejectedValue(
      new HttpErrorResponse({
        status: 400,
        error: { message: 'Ein Team mit diesem Namen gibt es schon.' },
      }),
    );

    const ok = await service.add('rakete');

    expect(ok).toBe(false);
    expect(service.error()).toBe('Ein Team mit diesem Namen gibt es schon.');
    expect(repository.list).not.toHaveBeenCalled();
  });

  it('zeigt die allgemeine Meldung, wenn der Server keine Meldung liefert (US-6)', async () => {
    repository.add.mockRejectedValue(new HttpErrorResponse({ status: 500, error: 'boom' }));

    const ok = await service.add('Rakete');

    expect(ok).toBe(false);
    expect(service.error()).toBe(GENERIC_ERROR);
  });

  it('vergisst eine alte Meldung, sobald ein Erfassen gelingt (US-6)', async () => {
    repository.add
      .mockRejectedValueOnce(
        new HttpErrorResponse({
          status: 400,
          error: { message: 'Ein Team mit diesem Namen gibt es schon.' },
        }),
      )
      .mockResolvedValueOnce(ADLER);
    repository.list.mockResolvedValue([ADLER]);
    await service.add('rakete');

    const ok = await service.add('Adler');

    expect(ok).toBe(true);
    expect(service.error()).toBeUndefined();
  });

  it('benennt ein Team um und lädt die Liste neu (US-6)', async () => {
    repository.rename.mockResolvedValue(KOMET);
    repository.list.mockResolvedValue([KOMET, ZEBRA]);

    const ok = await service.rename('r', 'Komet');

    expect(ok).toBe(true);
    expect(repository.rename).toHaveBeenCalledWith('r', 'Komet');
    expect(service.teams()).toEqual([KOMET, ZEBRA]);
  });

  it('zeigt die Meldung des Servers, wenn das Umbenennen abgelehnt wird (US-6)', async () => {
    repository.rename.mockRejectedValue(
      new HttpErrorResponse({
        status: 400,
        error: { message: 'Ein Team mit diesem Namen gibt es schon.' },
      }),
    );

    const ok = await service.rename('r', 'adler');

    expect(ok).toBe(false);
    expect(service.error()).toBe('Ein Team mit diesem Namen gibt es schon.');
    expect(repository.list).not.toHaveBeenCalled();
  });

  it('entfernt ein Team und lädt die Liste neu (US-6)', async () => {
    repository.list.mockResolvedValueOnce([RAKETE, ZEBRA]).mockResolvedValueOnce([ZEBRA]);
    repository.remove.mockResolvedValue(undefined);
    await service.load();

    const ok = await service.remove('r');

    expect(ok).toBe(true);
    expect(repository.remove).toHaveBeenCalledWith('r');
    expect(service.teams()).toEqual([ZEBRA]);
  });

  it('zeigt die Meldung des Servers, wenn das Team nicht mehr existiert (US-6)', async () => {
    repository.remove.mockRejectedValue(
      new HttpErrorResponse({ status: 404, error: { message: 'Das Team gibt es nicht.' } }),
    );

    const ok = await service.remove('unbekannt');

    expect(ok).toBe(false);
    expect(service.error()).toBe('Das Team gibt es nicht.');
    expect(repository.list).not.toHaveBeenCalled();
  });
});
