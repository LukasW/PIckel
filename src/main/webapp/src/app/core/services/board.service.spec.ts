import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Board } from '../models/board.model';
import { Feature } from '../models/feature.model';
import { BoardRepository } from '../ports/board.repository';
import { FeaturesRepository } from '../ports/features.repository';
import { BoardService } from './board.service';

const GENERIC_ERROR = 'Das hat nicht geklappt. Bitte versuche es nochmals.';
const PI_NOT_FOUND = 'Das PI gibt es nicht.';

const BOARD: Board = {
  teams: [
    { id: 'a', name: 'Adler' },
    { id: 'z', name: 'Zebra' },
  ],
  iterations: [
    {
      number: 1,
      name: 'Iteration 1',
      startDate: '2027-01-04',
      endDate: '2027-01-17',
      innovationAndPlanning: false,
    },
    {
      number: 2,
      name: 'IP',
      startDate: '2027-01-18',
      endDate: '2027-01-29',
      innovationAndPlanning: true,
    },
  ],
  backlog: [
    {
      id: 'f1',
      programIncrementId: 'pi-7',
      title: 'Portal',
      description: '',
      owner: '',
      wsjfRank: 1,
      estimate: 8,
      scheduled: false,
    },
  ],
  scheduled: [],
  milestones: [],
};

describe('BoardService', () => {
  let repository: { get: ReturnType<typeof vi.fn> };
  let features: { schedule: ReturnType<typeof vi.fn>; unschedule: ReturnType<typeof vi.fn> };
  let service: BoardService;

  beforeEach(() => {
    repository = { get: vi.fn() };
    features = { schedule: vi.fn(), unschedule: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: BoardRepository, useValue: repository },
        { provide: FeaturesRepository, useValue: features },
      ],
    });
    service = TestBed.inject(BoardService);
  });

  it('lädt das Board des gewählten PI (US-12)', async () => {
    repository.get.mockResolvedValue(BOARD);

    await service.load('pi-7');

    expect(repository.get).toHaveBeenCalledWith('pi-7');
    expect(service.board()).toEqual(BOARD);
    expect(service.error()).toBeUndefined();
    expect(service.loading()).toBe(false);
  });

  it('meldet das Laden, solange der Server nicht geantwortet hat (US-12)', async () => {
    let answer: (board: Board) => void = () => undefined;
    repository.get.mockReturnValue(new Promise<Board>((resolve) => (answer = resolve)));

    const loading = service.load('pi-7');

    expect(service.loading()).toBe(true);
    answer(BOARD);
    await loading;
    expect(service.loading()).toBe(false);
  });

  it('zeigt die Meldung des Servers, wenn es das PI nicht gibt (US-12)', async () => {
    repository.get.mockRejectedValue(
      new HttpErrorResponse({ status: 404, error: { message: PI_NOT_FOUND } }),
    );

    await service.load('unbekannt');

    expect(service.error()).toBe(PI_NOT_FOUND);
    expect(service.board()).toBeUndefined();
    expect(service.loading()).toBe(false);
  });

  it('zeigt die allgemeine Meldung, wenn der Server keine Meldung liefert (US-12)', async () => {
    repository.get.mockRejectedValue(new HttpErrorResponse({ status: 500, error: 'boom' }));

    await service.load('pi-7');

    expect(service.error()).toBe(GENERIC_ERROR);
  });

  it('zeigt die allgemeine Meldung, wenn das Laden ohne Antwort scheitert (US-12)', async () => {
    repository.get.mockRejectedValue(new Error('offline'));

    await service.load('pi-7');

    expect(service.error()).toBe(GENERIC_ERROR);
  });

  it('zeigt nach einem Wechsel das zuletzt angeforderte PI, auch wenn das frühere später antwortet (US-12)', async () => {
    const other: Board = { ...BOARD, teams: [{ id: 'k', name: 'Komet' }] };
    let answerFirst: (board: Board) => void = () => undefined;
    repository.get
      .mockReturnValueOnce(new Promise<Board>((resolve) => (answerFirst = resolve)))
      .mockResolvedValueOnce(other);

    const first = service.load('pi-7');
    await service.load('pi-8');
    answerFirst(BOARD);
    await first;

    expect(service.board()).toEqual(other);
    expect(service.loading()).toBe(false);
  });

  it('vergisst eine alte Meldung, sobald das Laden gelingt (US-12)', async () => {
    repository.get
      .mockRejectedValueOnce(
        new HttpErrorResponse({ status: 404, error: { message: PI_NOT_FOUND } }),
      )
      .mockResolvedValueOnce(BOARD);
    await service.load('unbekannt');

    await service.load('pi-7');

    expect(service.error()).toBeUndefined();
    expect(service.board()).toEqual(BOARD);
  });

  // --- US-13: Einplanen, optimistisch mit Rücknahme ---

  describe('Einplanen (US-13)', () => {
    const PI = 'pi-27-1';
    const IP_REJECTED = 'In der IP-Iteration wird keine Feature-Arbeit geplant.';

    function card(id: string, title: string, wsjfRank: number, cell?: [string, number]): Feature {
      return {
        id,
        programIncrementId: PI,
        title,
        description: '',
        owner: '',
        wsjfRank,
        scheduled: cell !== undefined,
        ...(cell ? { teamId: cell[0], iterationNumber: cell[1] } : {}),
      };
    }

    const PORTAL = card('f1', 'Portal', 1);
    const LOGIN = card('f2', 'Login', 2);
    // «Suche» liegt bei Adler in Iteration 2; so lässt sich Verschieben und Zurücknehmen prüfen.
    const SUCHE = card('f3', 'Suche', 3, ['a', 2]);

    /** PI 27.1 mit Iteration 1–3 und der IP als Nummer 4; Teams Adler und Zebra. */
    const PLANNING: Board = {
      teams: [
        { id: 'a', name: 'Adler' },
        { id: 'z', name: 'Zebra' },
      ],
      iterations: [
        ['Iteration 1', '2027-01-04', '2027-01-17'],
        ['Iteration 2', '2027-01-18', '2027-01-31'],
        ['Iteration 3', '2027-02-01', '2027-02-14'],
        ['IP', '2027-02-15', '2027-02-26'],
      ].map(([name, startDate, endDate], i) => ({
        number: i + 1,
        name,
        startDate,
        endDate,
        innovationAndPlanning: name === 'IP',
      })),
      backlog: [PORTAL, LOGIN],
      scheduled: [SUCHE],
      milestones: [],
    };

    async function loaded(): Promise<Board> {
      repository.get.mockResolvedValue(PLANNING);
      await service.load(PI);
      return structuredClone(service.board()!);
    }

    /** Ein Versprechen, das erst antwortet, wenn der Test es sagt. */
    function pending<T>(): { promise: Promise<T>; resolve: (v: T) => void } {
      let resolve: (v: T) => void = () => undefined;
      const promise = new Promise<T>((r) => (resolve = r));
      return { promise, resolve };
    }

    function ids(list: readonly Feature[]): string[] {
      return list.map((f) => f.id);
    }

    function scheduledEntry(id: string): Feature | undefined {
      return service.board()!.scheduled.find((f) => f.id === id);
    }

    it('plant das Feature beim Server in Team und Iterationsnummer des PI ein (US-13)', async () => {
      await loaded();
      features.schedule.mockResolvedValue({
        ...PORTAL,
        scheduled: true,
        teamId: 'z',
        iterationNumber: 3,
      });

      await service.schedule(PORTAL, 'z', 3);

      expect(features.schedule).toHaveBeenCalledWith(PI, 'f1', 'z', 3);
    });

    it('zeigt die Karte sofort in der Zelle, bevor der Server antwortet (US-13)', async () => {
      await loaded();
      const answer = pending<Feature>();
      features.schedule.mockReturnValue(answer.promise);

      const scheduling = service.schedule(PORTAL, 'z', 3);
      // Verhindert eine unbehandelte Ablehnung, falls die Prüfung davor scheitert; `await scheduling` meldet sie trotzdem.
      scheduling.catch(() => undefined);

      expect(scheduledEntry('f1')).toEqual(
        expect.objectContaining({ id: 'f1', teamId: 'z', iterationNumber: 3 }),
      );
      expect(ids(service.board()!.backlog)).toEqual(['f2']);
      answer.resolve({ ...PORTAL, scheduled: true, teamId: 'z', iterationNumber: 3 });
      await scheduling;
    });

    it('lässt die Karte nach dem Speichern in der Zelle (US-13)', async () => {
      await loaded();
      features.schedule.mockResolvedValue({
        ...PORTAL,
        scheduled: true,
        teamId: 'z',
        iterationNumber: 3,
      });

      await service.schedule(PORTAL, 'z', 3);

      expect(scheduledEntry('f1')).toEqual(
        expect.objectContaining({ id: 'f1', teamId: 'z', iterationNumber: 3 }),
      );
      expect(ids(service.board()!.backlog)).toEqual(['f2']);
      expect(service.actionError()).toBeUndefined();
    });

    it('verschiebt eine eingeplante Karte sofort in die neue Zelle, nur dorthin (US-13)', async () => {
      await loaded();
      const answer = pending<Feature>();
      features.schedule.mockReturnValue(answer.promise);

      const moving = service.schedule(SUCHE, 'z', 1);
      // Verhindert eine unbehandelte Ablehnung, falls die Prüfung davor scheitert; `await moving` meldet sie trotzdem.
      moving.catch(() => undefined);

      const entries = service.board()!.scheduled.filter((f) => f.id === 'f3');
      expect(entries).toEqual([expect.objectContaining({ teamId: 'z', iterationNumber: 1 })]);
      expect(ids(service.board()!.backlog)).toEqual(['f1', 'f2']);
      answer.resolve({ ...SUCHE, teamId: 'z', iterationNumber: 1 });
      await moving;
      expect(features.schedule).toHaveBeenCalledWith(PI, 'f3', 'z', 1);
    });

    it('nimmt das Einplanen bei einem Fehler zurück und zeigt die Meldung des Servers (US-13)', async () => {
      const before = await loaded();
      features.schedule.mockRejectedValue(
        new HttpErrorResponse({ status: 400, error: { message: IP_REJECTED } }),
      );

      await service.schedule(PORTAL, 'a', 4);

      expect(service.board()).toEqual(before);
      expect(service.actionError()).toBe(IP_REJECTED);
    });

    it('springt beim Verschieben bei einem Fehler in die alte Zelle zurück (US-13)', async () => {
      const before = await loaded();
      features.schedule.mockRejectedValue(
        new HttpErrorResponse({ status: 404, error: { message: 'Das Team gibt es nicht.' } }),
      );

      await service.schedule(SUCHE, 'z', 1);

      expect(service.board()).toEqual(before);
      expect(scheduledEntry('f3')).toEqual(
        expect.objectContaining({ teamId: 'a', iterationNumber: 2 }),
      );
      expect(service.actionError()).toBe('Das Team gibt es nicht.');
    });

    it('nimmt bei einem Fehler nur diese Karte zurück, nicht eine inzwischen gelungene Aktion (US-13)', async () => {
      await loaded();
      let rejectFirst: (reason: unknown) => void = () => undefined;
      features.schedule
        .mockImplementationOnce(() => new Promise((_, reject) => (rejectFirst = reject)))
        .mockResolvedValueOnce(undefined);

      const first = service.schedule(PORTAL, 'z', 3);
      await service.schedule(LOGIN, 'z', 1);
      rejectFirst(new HttpErrorResponse({ status: 400, error: { message: IP_REJECTED } }));
      await first;

      expect(service.board()?.backlog.map((f) => f.id)).toEqual(['f1']);
      expect(scheduledEntry('f2')).toEqual(
        expect.objectContaining({ teamId: 'z', iterationNumber: 1 }),
      );
      expect(service.actionError()).toBe(IP_REJECTED);
    });

    it('zeigt die allgemeine Meldung, wenn der Server beim Einplanen keine Meldung liefert (US-13)', async () => {
      const before = await loaded();
      features.schedule.mockRejectedValue(new HttpErrorResponse({ status: 500, error: 'boom' }));

      await service.schedule(PORTAL, 'z', 3);

      expect(service.board()).toEqual(before);
      expect(service.actionError()).toBe(GENERIC_ERROR);
    });

    it('zeigt die allgemeine Meldung, wenn das Einplanen ohne Antwort scheitert (US-13)', async () => {
      const before = await loaded();
      features.schedule.mockRejectedValue(new Error('offline'));

      await service.schedule(PORTAL, 'z', 3);

      expect(service.board()).toEqual(before);
      expect(service.actionError()).toBe(GENERIC_ERROR);
    });

    it('nimmt das Feature beim Server zurück ins Backlog (US-13)', async () => {
      await loaded();
      features.unschedule.mockResolvedValue(card('f3', 'Suche', 3));

      await service.unschedule(SUCHE);

      expect(features.unschedule).toHaveBeenCalledWith(PI, 'f3');
    });

    it('zeigt die Karte sofort wieder im Backlog, bevor der Server antwortet (US-13)', async () => {
      await loaded();
      const answer = pending<Feature>();
      features.unschedule.mockReturnValue(answer.promise);

      const unscheduling = service.unschedule(SUCHE);
      // Verhindert eine unbehandelte Ablehnung, falls die Prüfung davor scheitert; `await unscheduling` meldet sie trotzdem.
      unscheduling.catch(() => undefined);

      expect(scheduledEntry('f3')).toBeUndefined();
      const back = service.board()!.backlog.find((f) => f.id === 'f3');
      expect(back, 'Suche im Backlog').toBeDefined();
      expect(back!.teamId).toBeUndefined();
      expect(back!.iterationNumber).toBeUndefined();
      expect(ids(service.board()!.backlog)).toEqual(expect.arrayContaining(['f1', 'f2', 'f3']));
      answer.resolve(card('f3', 'Suche', 3));
      await unscheduling;
    });

    it('lässt die Karte nach dem Zurücknehmen im Backlog (US-13)', async () => {
      await loaded();
      features.unschedule.mockResolvedValue(card('f3', 'Suche', 3));

      await service.unschedule(SUCHE);

      expect(scheduledEntry('f3')).toBeUndefined();
      expect(ids(service.board()!.backlog)).toEqual(expect.arrayContaining(['f1', 'f2', 'f3']));
      expect(service.board()!.backlog).toHaveLength(3);
      expect(service.actionError()).toBeUndefined();
    });

    it('lässt die Karte bei einem Fehler beim Zurücknehmen in ihrer Zelle und zeigt die Meldung (US-13)', async () => {
      const before = await loaded();
      features.unschedule.mockRejectedValue(
        new HttpErrorResponse({ status: 404, error: { message: 'Das Feature gibt es nicht.' } }),
      );

      await service.unschedule(SUCHE);

      expect(service.board()).toEqual(before);
      expect(service.actionError()).toBe('Das Feature gibt es nicht.');
    });

    it('zeigt die allgemeine Meldung, wenn das Zurücknehmen ohne Meldung scheitert (US-13)', async () => {
      const before = await loaded();
      features.unschedule.mockRejectedValue(new Error('offline'));

      await service.unschedule(SUCHE);

      expect(service.board()).toEqual(before);
      expect(service.actionError()).toBe(GENERIC_ERROR);
    });
  });
});
