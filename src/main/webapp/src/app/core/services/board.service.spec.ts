import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Board } from '../models/board.model';
import { BoardRepository } from '../ports/board.repository';
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
  milestones: [],
};

describe('BoardService', () => {
  let repository: { get: ReturnType<typeof vi.fn> };
  let service: BoardService;

  beforeEach(() => {
    repository = { get: vi.fn() };
    TestBed.configureTestingModule({
      providers: [{ provide: BoardRepository, useValue: repository }],
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
});
