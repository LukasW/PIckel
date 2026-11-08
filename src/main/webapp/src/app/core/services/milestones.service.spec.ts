import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Milestone } from '../models/milestone.model';
import { MilestonesRepository } from '../ports/milestones.repository';
import { MilestonesService } from './milestones.service';

const PI_ID = 'pi-27-1';
const GENERIC_ERROR = 'Das hat nicht geklappt. Bitte versuche es nochmals.';
const OUTSIDE_PI = 'Das Datum muss im Zeitraum des PI liegen.';
const NAME_EMPTY = 'Der Name darf nicht leer sein.';

// PI 27.1 läuft vom 04.01. bis 26.03.2027 (Delta US-15).
const KICKOFF: Milestone = { id: 'k', name: 'Kickoff', date: '2027-01-11' };
const RELEASE: Milestone = { id: 'r', name: 'Release 4.0', date: '2027-02-10' };
const AUDIT: Milestone = { id: 'a', name: 'Audit', date: '2027-03-01' };

function rejected(message: string): HttpErrorResponse {
  return new HttpErrorResponse({ status: 400, error: { message } });
}

describe('MilestonesService', () => {
  let repository: {
    list: ReturnType<typeof vi.fn>;
    add: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    remove: ReturnType<typeof vi.fn>;
  };
  let service: MilestonesService;

  beforeEach(() => {
    repository = { list: vi.fn(), add: vi.fn(), update: vi.fn(), remove: vi.fn() };
    TestBed.configureTestingModule({
      providers: [{ provide: MilestonesRepository, useValue: repository }],
    });
    service = TestBed.inject(MilestonesService);
  });

  /** Lädt die Milestones, die der Server danach bei jedem weiteren Laden unverändert liefert. */
  async function loaded(...milestones: Milestone[]): Promise<void> {
    repository.list.mockResolvedValue(milestones);
    await service.load(PI_ID);
  }

  // --- Laden ---

  it('lädt die Milestones des gewählten PI (US-15)', async () => {
    repository.list.mockResolvedValue([KICKOFF, RELEASE]);

    await service.load('pi-8');

    expect(repository.list).toHaveBeenCalledWith('pi-8');
    expect(service.milestones()).toEqual([KICKOFF, RELEASE]);
    expect(service.error()).toBeUndefined();
  });

  it('zeigt die allgemeine Meldung, wenn das Laden scheitert (US-15)', async () => {
    repository.list.mockRejectedValue(new Error('offline'));

    await service.load(PI_ID);

    expect(service.error()).toBe(GENERIC_ERROR);
  });

  // --- AK 1: erfassen ---

  it('erfasst einen Milestone und reiht ihn nach Datum in die Liste ein (US-15)', async () => {
    await loaded(KICKOFF, AUDIT);
    repository.add.mockResolvedValue(RELEASE);

    const ok = await service.add(PI_ID, { name: 'Release 4.0', date: '2027-02-10' });

    expect(ok).toBe(true);
    expect(repository.add).toHaveBeenCalledWith(PI_ID, { name: 'Release 4.0', date: '2027-02-10' });
    expect(service.milestones()).toEqual([KICKOFF, RELEASE, AUDIT]);
    expect(service.error()).toBeUndefined();
  });

  it('reiht bei gleichem Datum nach Name ein (US-15)', async () => {
    const messe: Milestone = { id: 'm', name: 'Messe', date: '2027-02-10' };
    const audit: Milestone = { id: 'a2', name: 'Audit', date: '2027-02-10' };
    await loaded(messe);
    repository.add.mockResolvedValue(audit);

    await service.add(PI_ID, { name: 'Audit', date: '2027-02-10' });

    expect(service.milestones()).toEqual([audit, messe]);
  });

  it('zeigt die Meldung des Servers und lässt die Liste, wenn das Datum ausserhalb des PI liegt (US-15)', async () => {
    await loaded(KICKOFF);
    repository.add.mockRejectedValue(rejected(OUTSIDE_PI));

    const ok = await service.add(PI_ID, { name: 'Release 4.0', date: '2027-04-01' });

    expect(ok).toBe(false);
    expect(service.error()).toBe(OUTSIDE_PI);
    expect(service.milestones()).toEqual([KICKOFF]);
  });

  it('zeigt die Meldung des Servers, wenn ein Milestone ohne Namen abgelehnt wird (US-15)', async () => {
    repository.add.mockRejectedValue(rejected(NAME_EMPTY));

    const ok = await service.add(PI_ID, { name: ' ', date: '2027-02-10' });

    expect(ok).toBe(false);
    expect(service.error()).toBe(NAME_EMPTY);
  });

  it('zeigt die allgemeine Meldung, wenn der Server keine Meldung liefert (US-15)', async () => {
    repository.add.mockRejectedValue(new HttpErrorResponse({ status: 500, error: 'boom' }));

    const ok = await service.add(PI_ID, { name: 'Release 4.0', date: '2027-02-10' });

    expect(ok).toBe(false);
    expect(service.error()).toBe(GENERIC_ERROR);
  });

  // --- AK 3: ändern ---

  it('ersetzt den geänderten Milestone und reiht ihn nach dem neuen Datum ein (US-15)', async () => {
    await loaded(KICKOFF, RELEASE);
    const moved: Milestone = { ...RELEASE, date: '2027-01-05' };
    repository.update.mockResolvedValue(moved);

    const ok = await service.update(PI_ID, 'r', { name: 'Release 4.0', date: '2027-01-05' });

    expect(ok).toBe(true);
    expect(repository.update).toHaveBeenCalledWith(PI_ID, 'r', {
      name: 'Release 4.0',
      date: '2027-01-05',
    });
    expect(service.milestones()).toEqual([moved, KICKOFF]);
    expect(service.error()).toBeUndefined();
  });

  it('übernimmt beim Ändern den Namen, den der Server zurückgibt (US-15)', async () => {
    await loaded(KICKOFF, RELEASE);
    const renamed: Milestone = { ...RELEASE, name: 'Release 4.1' };
    repository.update.mockResolvedValue(renamed);

    await service.update(PI_ID, 'r', { name: 'Release 4.1', date: '2027-02-10' });

    expect(service.milestones()).toEqual([KICKOFF, renamed]);
  });

  it('lässt die Liste, wenn das Ändern abgelehnt wird (US-15)', async () => {
    await loaded(KICKOFF, RELEASE);
    repository.update.mockRejectedValue(rejected(OUTSIDE_PI));

    const ok = await service.update(PI_ID, 'r', { name: 'Release 4.0', date: '2027-04-01' });

    expect(ok).toBe(false);
    expect(service.error()).toBe(OUTSIDE_PI);
    expect(service.milestones()).toEqual([KICKOFF, RELEASE]);
  });

  // --- AK 3: löschen ---

  it('entfernt genau den gelöschten Milestone (US-15)', async () => {
    await loaded(KICKOFF, RELEASE, AUDIT);
    repository.remove.mockResolvedValue(undefined);

    const ok = await service.remove(PI_ID, 'r');

    expect(ok).toBe(true);
    expect(repository.remove).toHaveBeenCalledWith(PI_ID, 'r');
    expect(service.milestones()).toEqual([KICKOFF, AUDIT]);
    expect(service.error()).toBeUndefined();
  });

  it('behält den Milestone, wenn das Löschen scheitert (US-15)', async () => {
    await loaded(KICKOFF, RELEASE);
    repository.remove.mockRejectedValue(
      new HttpErrorResponse({ status: 404, error: { message: 'Den Milestone gibt es nicht.' } }),
    );

    const ok = await service.remove(PI_ID, 'r');

    expect(ok).toBe(false);
    expect(service.error()).toBe('Den Milestone gibt es nicht.');
    expect(service.milestones()).toEqual([KICKOFF, RELEASE]);
  });

  // --- Meldung zurücksetzen ---

  it('vergisst eine alte Meldung, sobald ein Erfassen gelingt (US-15)', async () => {
    await loaded();
    repository.add.mockRejectedValueOnce(rejected(OUTSIDE_PI)).mockResolvedValueOnce(RELEASE);
    await service.add(PI_ID, { name: 'Release 4.0', date: '2027-04-01' });

    const ok = await service.add(PI_ID, { name: 'Release 4.0', date: '2027-02-10' });

    expect(ok).toBe(true);
    expect(service.error()).toBeUndefined();
    expect(service.milestones()).toEqual([RELEASE]);
  });

  it('vergisst eine alte Meldung, sobald ein Ändern gelingt (US-15)', async () => {
    await loaded(RELEASE);
    repository.add.mockRejectedValue(rejected(OUTSIDE_PI));
    repository.update.mockResolvedValue(RELEASE);
    await service.add(PI_ID, { name: 'Release 4.0', date: '2027-04-01' });

    const ok = await service.update(PI_ID, 'r', { name: 'Release 4.0', date: '2027-02-10' });

    expect(ok).toBe(true);
    expect(service.error()).toBeUndefined();
  });

  it('vergisst eine alte Meldung, sobald ein Löschen gelingt (US-15)', async () => {
    await loaded(RELEASE);
    repository.add.mockRejectedValue(rejected(OUTSIDE_PI));
    repository.remove.mockResolvedValue(undefined);
    await service.add(PI_ID, { name: 'Release 4.0', date: '2027-04-01' });

    await service.remove(PI_ID, 'r');

    expect(service.error()).toBeUndefined();
  });
});
