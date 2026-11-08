import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Feature } from '../models/feature.model';
import { FeaturesRepository } from '../ports/features.repository';
import { FeaturesService } from './features.service';

const GENERIC_ERROR = 'Das hat nicht geklappt. Bitte versuche es nochmals.';
const PI_ID = 'pi-27-1';

function feature(id: string, title: string, extra: Partial<Feature> = {}): Feature {
  return {
    id,
    programIncrementId: PI_ID,
    title,
    description: '',
    owner: '',
    scheduled: false,
    ...extra,
  };
}

const PORTAL = feature('p', 'Self-Service-Portal', { wsjfRank: 1, owner: 'Anna', estimate: 21 });
const LOGIN = feature('l', 'Login');
const ZAHLUNG = feature('z', 'Zahlung', { wsjfRank: 3 });

describe('FeaturesService', () => {
  let repository: {
    list: ReturnType<typeof vi.fn>;
    add: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    remove: ReturnType<typeof vi.fn>;
    importCsv: ReturnType<typeof vi.fn>;
  };
  let service: FeaturesService;

  beforeEach(() => {
    repository = {
      list: vi.fn(),
      add: vi.fn(),
      update: vi.fn(),
      remove: vi.fn(),
      importCsv: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [{ provide: FeaturesRepository, useValue: repository }],
    });
    service = TestBed.inject(FeaturesService);
  });

  it('lädt das Backlog des gewählten PI in der Reihenfolge des Servers (US-9)', async () => {
    // Bewusst nicht nach Rang: Der Client darf die Reihenfolge des Servers nicht umstellen.
    repository.list.mockResolvedValue([LOGIN, ZAHLUNG, PORTAL]);

    await service.load(PI_ID);

    expect(repository.list).toHaveBeenCalledWith(PI_ID);
    expect(service.features().map((f) => f.title)).toEqual([
      'Login',
      'Zahlung',
      'Self-Service-Portal',
    ]);
    expect(service.loading()).toBe(false);
    expect(service.error()).toBeUndefined();
  });

  it('zeigt die allgemeine Meldung, wenn das Laden scheitert (US-9)', async () => {
    repository.list.mockRejectedValue(new Error('offline'));

    await service.load(PI_ID);

    expect(service.error()).toBe(GENERIC_ERROR);
    expect(service.loading()).toBe(false);
  });

  it('erfasst ein Feature im gewählten PI und lädt dessen Backlog neu (US-9)', async () => {
    repository.list.mockResolvedValueOnce([LOGIN]).mockResolvedValueOnce([PORTAL, LOGIN]);
    repository.add.mockResolvedValue(PORTAL);
    await service.load(PI_ID);

    const ok = await service.add(PI_ID, {
      title: 'Self-Service-Portal',
      wsjfRank: 1,
      owner: 'Anna',
      estimate: 21,
    });

    expect(ok).toBe(true);
    expect(repository.add).toHaveBeenCalledWith(PI_ID, {
      title: 'Self-Service-Portal',
      wsjfRank: 1,
      owner: 'Anna',
      estimate: 21,
    });
    expect(repository.list).toHaveBeenCalledTimes(2);
    expect(repository.list).toHaveBeenLastCalledWith(PI_ID);
    expect(service.features()).toEqual([PORTAL, LOGIN]);
  });

  it('zeigt die Meldung des Servers, wenn das PI unbekannt ist (US-9)', async () => {
    repository.add.mockRejectedValue(
      new HttpErrorResponse({ status: 404, error: { message: 'Das PI gibt es nicht.' } }),
    );

    const ok = await service.add('unbekannt', { title: 'Login' });

    expect(ok).toBe(false);
    expect(service.error()).toBe('Das PI gibt es nicht.');
    expect(repository.list).not.toHaveBeenCalled();
  });

  it('zeigt die Meldung des Servers, wenn er ungültige Angaben ablehnt (US-9)', async () => {
    repository.add.mockRejectedValue(
      new HttpErrorResponse({
        status: 400,
        error: { message: 'Der WSJF-Rang muss eine ganze Zahl ab 1 sein.' },
      }),
    );

    const ok = await service.add(PI_ID, { title: 'Login', wsjfRank: 0 });

    expect(ok).toBe(false);
    expect(service.error()).toBe('Der WSJF-Rang muss eine ganze Zahl ab 1 sein.');
  });

  it('zeigt die allgemeine Meldung, wenn der Server keine Meldung liefert (US-9)', async () => {
    repository.add.mockRejectedValue(new HttpErrorResponse({ status: 500, error: 'boom' }));

    const ok = await service.add(PI_ID, { title: 'Login' });

    expect(ok).toBe(false);
    expect(service.error()).toBe(GENERIC_ERROR);
  });

  it('vergisst eine alte Meldung, sobald ein Erfassen gelingt (US-9)', async () => {
    repository.add
      .mockRejectedValueOnce(
        new HttpErrorResponse({ status: 404, error: { message: 'Das PI gibt es nicht.' } }),
      )
      .mockResolvedValueOnce(LOGIN);
    repository.list.mockResolvedValue([LOGIN]);
    await service.add(PI_ID, { title: 'Login' });

    const ok = await service.add(PI_ID, { title: 'Login' });

    expect(ok).toBe(true);
    expect(service.error()).toBeUndefined();
  });

  it('bearbeitet ein Feature im gewählten PI und lädt dessen Backlog neu (US-10)', async () => {
    const changed = { ...LOGIN, wsjfRank: 3, estimate: 8 };
    repository.list.mockResolvedValueOnce([LOGIN]).mockResolvedValueOnce([changed]);
    repository.update.mockResolvedValue(changed);
    await service.load(PI_ID);

    const ok = await service.update(PI_ID, 'l', { title: 'Login', wsjfRank: 3, estimate: 8 });

    expect(ok).toBe(true);
    expect(repository.update).toHaveBeenCalledWith(PI_ID, 'l', {
      title: 'Login',
      wsjfRank: 3,
      estimate: 8,
    });
    expect(repository.add).not.toHaveBeenCalled();
    expect(repository.list).toHaveBeenCalledTimes(2);
    expect(repository.list).toHaveBeenLastCalledWith(PI_ID);
    expect(service.features()).toEqual([changed]);
    expect(service.error()).toBeUndefined();
  });

  it('zeigt die Meldung des Servers, wenn er das Bearbeiten ablehnt (US-10)', async () => {
    repository.update.mockRejectedValue(
      new HttpErrorResponse({ status: 400, error: { message: 'Der Titel darf nicht leer sein.' } }),
    );

    const ok = await service.update(PI_ID, 'l', { title: ' ' });

    expect(ok).toBe(false);
    expect(service.error()).toBe('Der Titel darf nicht leer sein.');
    expect(repository.list).not.toHaveBeenCalled();
  });

  it('zeigt die allgemeine Meldung, wenn das Bearbeiten ohne Meldung scheitert (US-10)', async () => {
    repository.update.mockRejectedValue(new HttpErrorResponse({ status: 500, error: 'boom' }));

    const ok = await service.update(PI_ID, 'l', { title: 'Login' });

    expect(ok).toBe(false);
    expect(service.error()).toBe(GENERIC_ERROR);
  });

  it('vergisst eine alte Meldung, sobald ein Bearbeiten gelingt (US-10)', async () => {
    repository.update
      .mockRejectedValueOnce(
        new HttpErrorResponse({
          status: 400,
          error: { message: 'Der Titel darf nicht leer sein.' },
        }),
      )
      .mockResolvedValueOnce(LOGIN);
    repository.list.mockResolvedValue([LOGIN]);
    await service.update(PI_ID, 'l', { title: ' ' });

    const ok = await service.update(PI_ID, 'l', { title: 'Login' });

    expect(ok).toBe(true);
    expect(service.error()).toBeUndefined();
  });

  it('löscht ein Feature im gewählten PI und lädt dessen Backlog neu (US-10)', async () => {
    repository.list.mockResolvedValueOnce([LOGIN, PORTAL]).mockResolvedValueOnce([PORTAL]);
    repository.remove.mockResolvedValue(undefined);
    await service.load(PI_ID);

    const ok = await service.remove(PI_ID, 'l');

    expect(ok).toBe(true);
    expect(repository.remove).toHaveBeenCalledWith(PI_ID, 'l');
    expect(repository.list).toHaveBeenCalledTimes(2);
    expect(repository.list).toHaveBeenLastCalledWith(PI_ID);
    expect(service.features()).toEqual([PORTAL]);
    expect(service.error()).toBeUndefined();
  });

  it('zeigt die Meldung des Servers, wenn das Löschen scheitert (US-10)', async () => {
    repository.remove.mockRejectedValue(
      new HttpErrorResponse({ status: 404, error: { message: 'Das Feature gibt es nicht.' } }),
    );

    const ok = await service.remove(PI_ID, 'l');

    expect(ok).toBe(false);
    expect(service.error()).toBe('Das Feature gibt es nicht.');
    expect(repository.list).not.toHaveBeenCalled();
  });

  it('zeigt die allgemeine Meldung, wenn das Löschen ohne Meldung scheitert (US-10)', async () => {
    repository.remove.mockRejectedValue(new Error('offline'));

    const ok = await service.remove(PI_ID, 'l');

    expect(ok).toBe(false);
    expect(service.error()).toBe(GENERIC_ERROR);
  });

  // --- US-11: Features per CSV importieren ---

  const CSV = 'Titel;WSJF-Rang;Owner;Schätzung\nPortal;1;Anna;21\nSuche;2;Beat;13\n';
  const SUCHE = feature('s', 'Suche', { wsjfRank: 2, owner: 'Beat', estimate: 13 });
  const ROW_ERROR = 'Zeile 3: Der WSJF-Rang muss eine ganze Zahl ab 1 sein.';

  it('importiert die CSV ins gewählte PI, merkt sich die Anzahl und lädt das Backlog neu (US-11)', async () => {
    repository.list.mockResolvedValueOnce([LOGIN]).mockResolvedValueOnce([LOGIN, PORTAL, SUCHE]);
    repository.importCsv.mockResolvedValue(2);
    await service.load(PI_ID);

    const ok = await service.importCsv(PI_ID, CSV);

    expect(ok).toBe(true);
    expect(repository.importCsv).toHaveBeenCalledWith(PI_ID, CSV);
    expect(service.imported()).toBe(2);
    expect(repository.list).toHaveBeenCalledTimes(2);
    expect(repository.list).toHaveBeenLastCalledWith(PI_ID);
    expect(service.features()).toEqual([LOGIN, PORTAL, SUCHE]);
    expect(service.error()).toBeUndefined();
  });

  it('übernimmt die Anzahl des Servers, nicht die Zeilen der Datei (US-11)', async () => {
    // Der Server überspringt leere Zeilen; nur er weiss, wie viele Features es wurden.
    repository.list.mockResolvedValue([]);
    repository.importCsv.mockResolvedValue(1);

    await service.importCsv(PI_ID, 'Titel\nPortal\n\nSuche\n');

    expect(service.imported()).toBe(1);
  });

  it('zeigt die Meldung des Servers mit Zeilennummer, wenn er den Import ablehnt (US-11)', async () => {
    repository.importCsv.mockRejectedValue(
      new HttpErrorResponse({ status: 400, error: { message: ROW_ERROR } }),
    );

    const ok = await service.importCsv(PI_ID, 'Titel;WSJF-Rang\nPortal;1\nSuche;null\n');

    expect(ok).toBe(false);
    expect(service.error()).toBe(ROW_ERROR);
    expect(service.imported()).toBeUndefined();
    expect(repository.list).not.toHaveBeenCalled();
  });

  it('zeigt die allgemeine Meldung, wenn der Import ohne Meldung scheitert (US-11)', async () => {
    repository.importCsv.mockRejectedValue(new Error('offline'));

    const ok = await service.importCsv(PI_ID, CSV);

    expect(ok).toBe(false);
    expect(service.error()).toBe(GENERIC_ERROR);
  });

  it('vergisst die Anzahl eines früheren Imports, wenn ein neuer Import scheitert (US-11)', async () => {
    repository.list.mockResolvedValue([PORTAL, SUCHE]);
    repository.importCsv.mockResolvedValueOnce(2).mockRejectedValueOnce(
      new HttpErrorResponse({
        status: 400,
        error: { message: 'Die Kopfzeile braucht die Spalte «Titel».' },
      }),
    );
    await service.importCsv(PI_ID, CSV);

    const ok = await service.importCsv(PI_ID, 'Name;Owner\nPortal;Anna\n');

    expect(ok).toBe(false);
    expect(service.imported()).toBeUndefined();
    expect(service.error()).toBe('Die Kopfzeile braucht die Spalte «Titel».');
  });

  it('vergisst eine alte Meldung, sobald ein Import gelingt (US-11)', async () => {
    repository.list.mockResolvedValue([PORTAL]);
    repository.importCsv
      .mockRejectedValueOnce(new HttpErrorResponse({ status: 400, error: { message: ROW_ERROR } }))
      .mockResolvedValueOnce(1);
    await service.importCsv(PI_ID, CSV);

    const ok = await service.importCsv(PI_ID, 'Titel\nPortal\n');

    expect(ok).toBe(true);
    expect(service.error()).toBeUndefined();
    expect(service.imported()).toBe(1);
  });
});
