import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { FeatureDependency } from '../models/feature-dependency.model';
import { FeatureDependenciesRepository } from '../ports/feature-dependencies.repository';
import { FeatureDependenciesService } from './feature-dependencies.service';

const GENERIC_ERROR = 'Das hat nicht geklappt. Bitte versuche es nochmals.';
const PI_ID = 'pi-27-1';

// IDs der Features aus den Szenarien: Portal braucht Login.
const PORTAL = 'p';
const LOGIN = 'l';
const ZAHLUNG = 'z';
const SUCHE = 's';

function dependency(needing: string, needed: string): FeatureDependency {
  return { needingFeatureId: needing, neededFeatureId: needed };
}

const PORTAL_NEEDS_LOGIN = dependency(PORTAL, LOGIN);

describe('FeatureDependenciesService', () => {
  let repository: {
    list: ReturnType<typeof vi.fn>;
    add: ReturnType<typeof vi.fn>;
    remove: ReturnType<typeof vi.fn>;
  };
  let service: FeatureDependenciesService;

  beforeEach(() => {
    repository = { list: vi.fn(), add: vi.fn(), remove: vi.fn() };
    TestBed.configureTestingModule({
      providers: [{ provide: FeatureDependenciesRepository, useValue: repository }],
    });
    service = TestBed.inject(FeatureDependenciesService);
  });

  it('lädt die Abhängigkeiten des gewählten PI (US-16)', async () => {
    repository.list.mockResolvedValue([PORTAL_NEEDS_LOGIN]);

    await service.load(PI_ID);

    expect(repository.list).toHaveBeenCalledWith(PI_ID);
    expect(service.dependencies()).toEqual([PORTAL_NEEDS_LOGIN]);
    expect(service.error()).toBeUndefined();
  });

  it('zeigt die allgemeine Meldung, wenn das Laden scheitert (US-16)', async () => {
    repository.list.mockRejectedValue(new Error('offline'));

    await service.load(PI_ID);

    expect(service.error()).toBe(GENERIC_ERROR);
  });

  it('zeigt die Meldung des Servers, wenn er das Laden ablehnt (US-16)', async () => {
    repository.list.mockRejectedValue(
      new HttpErrorResponse({ status: 404, error: { message: 'Das PI gibt es nicht.' } }),
    );

    await service.load('unbekannt');

    expect(service.error()).toBe('Das PI gibt es nicht.');
  });

  it('nennt die Features, die ein Feature braucht, nicht die, die es brauchen (US-16)', async () => {
    // Portal braucht Login und Zahlung; Suche braucht Portal (andere Richtung); Zahlung braucht Login (fremd).
    repository.list.mockResolvedValue([
      dependency(PORTAL, LOGIN),
      dependency(SUCHE, PORTAL),
      dependency(ZAHLUNG, LOGIN),
      dependency(PORTAL, ZAHLUNG),
    ]);
    await service.load(PI_ID);

    expect([...service.needs(PORTAL)].sort()).toEqual([LOGIN, ZAHLUNG].sort());
  });

  it('nennt die Features, die ein Feature brauchen, nicht die, die es braucht (US-16)', async () => {
    // Login wird von Portal und Zahlung gebraucht; Login selbst braucht Suche (andere Richtung).
    repository.list.mockResolvedValue([
      dependency(PORTAL, LOGIN),
      dependency(LOGIN, SUCHE),
      dependency(PORTAL, ZAHLUNG),
      dependency(ZAHLUNG, LOGIN),
    ]);
    await service.load(PI_ID);

    expect([...service.neededBy(LOGIN)].sort()).toEqual([PORTAL, ZAHLUNG].sort());
  });

  it('nennt für ein Feature ohne Abhängigkeiten in keiner Richtung ein Feature (US-16)', async () => {
    repository.list.mockResolvedValue([PORTAL_NEEDS_LOGIN]);
    await service.load(PI_ID);

    expect(service.needs(SUCHE)).toEqual([]);
    expect(service.neededBy(SUCHE)).toEqual([]);
    // Szenario «Abhängigkeit wird erfasst»: Login braucht nichts, Portal wird von niemandem gebraucht.
    expect(service.needs(LOGIN)).toEqual([]);
    expect(service.neededBy(PORTAL)).toEqual([]);
  });

  it('hält fest, dass ein Feature ein anderes braucht, und lädt die Abhängigkeiten neu (US-16)', async () => {
    repository.list.mockResolvedValueOnce([]).mockResolvedValueOnce([PORTAL_NEEDS_LOGIN]);
    repository.add.mockResolvedValue(PORTAL_NEEDS_LOGIN);
    await service.load(PI_ID);

    const ok = await service.add(PI_ID, PORTAL, LOGIN);

    expect(ok).toBe(true);
    expect(repository.add).toHaveBeenCalledWith(PI_ID, PORTAL, LOGIN);
    expect(repository.list).toHaveBeenCalledTimes(2);
    expect(repository.list).toHaveBeenLastCalledWith(PI_ID);
    expect(service.needs(PORTAL)).toEqual([LOGIN]);
    expect(service.neededBy(LOGIN)).toEqual([PORTAL]);
    expect(service.error()).toBeUndefined();
  });

  it.each([
    ['ein Feature sich selbst braucht', 'Ein Feature kann nicht von sich selbst abhängen.'],
    ['die Abhängigkeit doppelt ist', 'Diese Abhängigkeit gibt es schon.'],
    [
      'die Abhängigkeit umgekehrt schon besteht',
      'Die beiden Features hängen schon umgekehrt voneinander ab.',
    ],
    ['das Feature zu einem anderen PI gehört', 'Beide Features müssen zum selben PI gehören.'],
  ])(
    'zeigt die Meldung des Servers und lädt nicht neu, wenn %s (US-16)',
    async (_fall: string, message: string) => {
      repository.add.mockRejectedValue(new HttpErrorResponse({ status: 400, error: { message } }));

      const ok = await service.add(PI_ID, PORTAL, LOGIN);

      expect(ok).toBe(false);
      expect(service.error()).toBe(message);
      expect(repository.list).not.toHaveBeenCalled();
    },
  );

  it('zeigt die allgemeine Meldung, wenn das Erfassen ohne Meldung scheitert (US-16)', async () => {
    repository.add.mockRejectedValue(new HttpErrorResponse({ status: 500, error: 'boom' }));

    const ok = await service.add(PI_ID, PORTAL, LOGIN);

    expect(ok).toBe(false);
    expect(service.error()).toBe(GENERIC_ERROR);
  });

  it('vergisst eine alte Meldung, sobald ein Erfassen gelingt (US-16)', async () => {
    repository.add
      .mockRejectedValueOnce(
        new HttpErrorResponse({
          status: 400,
          error: { message: 'Diese Abhängigkeit gibt es schon.' },
        }),
      )
      .mockResolvedValueOnce(dependency(PORTAL, ZAHLUNG));
    repository.list.mockResolvedValue([PORTAL_NEEDS_LOGIN, dependency(PORTAL, ZAHLUNG)]);
    await service.add(PI_ID, PORTAL, LOGIN);

    const ok = await service.add(PI_ID, PORTAL, ZAHLUNG);

    expect(ok).toBe(true);
    expect(service.error()).toBeUndefined();
  });

  it('entfernt eine Abhängigkeit und lädt die Abhängigkeiten neu (US-16)', async () => {
    repository.list.mockResolvedValueOnce([PORTAL_NEEDS_LOGIN]).mockResolvedValueOnce([]);
    repository.remove.mockResolvedValue(undefined);
    await service.load(PI_ID);

    const ok = await service.remove(PI_ID, PORTAL, LOGIN);

    expect(ok).toBe(true);
    expect(repository.remove).toHaveBeenCalledWith(PI_ID, PORTAL, LOGIN);
    expect(repository.list).toHaveBeenCalledTimes(2);
    expect(repository.list).toHaveBeenLastCalledWith(PI_ID);
    expect(service.needs(PORTAL)).toEqual([]);
    expect(service.neededBy(LOGIN)).toEqual([]);
    expect(service.error()).toBeUndefined();
  });

  it('zeigt die Meldung des Servers und lädt nicht neu, wenn er das Entfernen ablehnt (US-16)', async () => {
    // Die Meldung ist in der Spezifikation nicht festgelegt; geprüft wird, dass sie durchgereicht wird.
    const message = 'Meldung des Servers zum Entfernen';
    repository.remove.mockRejectedValue(new HttpErrorResponse({ status: 404, error: { message } }));

    const ok = await service.remove(PI_ID, PORTAL, LOGIN);

    expect(ok).toBe(false);
    expect(service.error()).toBe(message);
    expect(repository.list).not.toHaveBeenCalled();
  });

  it('zeigt die allgemeine Meldung, wenn das Entfernen ohne Meldung scheitert (US-16)', async () => {
    repository.remove.mockRejectedValue(new Error('offline'));

    const ok = await service.remove(PI_ID, PORTAL, LOGIN);

    expect(ok).toBe(false);
    expect(service.error()).toBe(GENERIC_ERROR);
  });

  it('vergisst eine alte Meldung, sobald ein Entfernen gelingt (US-16)', async () => {
    repository.add.mockRejectedValue(
      new HttpErrorResponse({
        status: 400,
        error: { message: 'Diese Abhängigkeit gibt es schon.' },
      }),
    );
    repository.remove.mockResolvedValue(undefined);
    repository.list.mockResolvedValue([]);
    await service.add(PI_ID, PORTAL, LOGIN);

    const ok = await service.remove(PI_ID, PORTAL, LOGIN);

    expect(ok).toBe(true);
    expect(service.error()).toBeUndefined();
  });

  it('vergisst eine alte Meldung, sobald ein Laden gelingt (US-16)', async () => {
    repository.add.mockRejectedValue(
      new HttpErrorResponse({
        status: 400,
        error: { message: 'Diese Abhängigkeit gibt es schon.' },
      }),
    );
    repository.list.mockResolvedValue([PORTAL_NEEDS_LOGIN]);
    await service.add(PI_ID, PORTAL, LOGIN);

    await service.load(PI_ID);

    expect(service.error()).toBeUndefined();
  });
});
