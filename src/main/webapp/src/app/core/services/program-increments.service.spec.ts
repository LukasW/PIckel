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
  businessContext: '',
  artObjectives: '',
  links: [],
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
  businessContext: '',
  artObjectives: '',
  links: [],
};

const VISION = { id: 'l1', title: 'Vision', url: 'https://wiki.example.ch/vision' };

describe('ProgramIncrementsService', () => {
  let repository: {
    list: ReturnType<typeof vi.fn>;
    get: ReturnType<typeof vi.fn>;
    plan: ReturnType<typeof vi.fn>;
    updateBusinessContext: ReturnType<typeof vi.fn>;
    addLink: ReturnType<typeof vi.fn>;
    removeLink: ReturnType<typeof vi.fn>;
  };
  let service: ProgramIncrementsService;

  beforeEach(() => {
    repository = {
      list: vi.fn(),
      get: vi.fn(),
      plan: vi.fn(),
      updateBusinessContext: vi.fn(),
      addLink: vi.fn(),
      removeLink: vi.fn(),
    };
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

  it('speichert Business-Kontext und PI Objectives und liefert das geänderte PI (US-8)', async () => {
    const changed = {
      ...PI_27_1,
      businessContext: 'Fokus auf Self-Service',
      artObjectives: 'Checkout in 2 Schritten',
    };
    repository.updateBusinessContext.mockResolvedValue(changed);

    const pi = await service.updateBusinessContext('1', {
      businessContext: 'Fokus auf Self-Service',
      artObjectives: 'Checkout in 2 Schritten',
    });

    expect(repository.updateBusinessContext).toHaveBeenCalledWith('1', {
      businessContext: 'Fokus auf Self-Service',
      artObjectives: 'Checkout in 2 Schritten',
    });
    expect(pi).toEqual(changed);
    expect(service.error()).toBeUndefined();
  });

  it('liefert kein PI und zeigt die Meldung des Servers, wenn der Text zu lang ist (US-8)', async () => {
    repository.updateBusinessContext.mockRejectedValue(
      new HttpErrorResponse({
        status: 400,
        error: { message: 'Der Text darf höchstens 10 000 Zeichen lang sein.' },
      }),
    );

    const pi = await service.updateBusinessContext('1', {
      businessContext: 'x'.repeat(10_001),
      artObjectives: '',
    });

    expect(pi).toBeUndefined();
    expect(service.error()).toBe('Der Text darf höchstens 10 000 Zeichen lang sein.');
  });

  it('fügt einen Link hinzu und liefert das PI mit dem Link (US-8)', async () => {
    const changed = { ...PI_27_1, links: [VISION] };
    repository.addLink.mockResolvedValue(changed);

    const pi = await service.addLink('1', {
      title: 'Vision',
      url: 'https://wiki.example.ch/vision',
    });

    expect(repository.addLink).toHaveBeenCalledWith('1', {
      title: 'Vision',
      url: 'https://wiki.example.ch/vision',
    });
    expect(pi).toEqual(changed);
    expect(service.error()).toBeUndefined();
  });

  it('liefert kein PI und zeigt die Meldung des Servers bei einer URL ohne http oder https (US-8)', async () => {
    repository.addLink.mockRejectedValue(
      new HttpErrorResponse({
        status: 400,
        error: { message: 'Die URL muss mit http:// oder https:// beginnen.' },
      }),
    );

    const pi = await service.addLink('1', { title: 'Vision', url: 'javascript:alert(1)' });

    expect(pi).toBeUndefined();
    expect(service.error()).toBe('Die URL muss mit http:// oder https:// beginnen.');
  });

  it('vergisst die Meldung eines abgelehnten Links, sobald ein Link hinzugefügt wird (US-8)', async () => {
    repository.addLink
      .mockRejectedValueOnce(
        new HttpErrorResponse({
          status: 400,
          error: { message: 'Die URL muss mit http:// oder https:// beginnen.' },
        }),
      )
      .mockResolvedValueOnce({ ...PI_27_1, links: [VISION] });
    await service.addLink('1', { title: 'Vision', url: 'javascript:alert(1)' });

    await service.addLink('1', { title: 'Vision', url: 'https://wiki.example.ch/vision' });

    expect(service.error()).toBeUndefined();
  });

  it('entfernt einen Link und liefert das PI ohne den Link (US-8)', async () => {
    repository.removeLink.mockResolvedValue(PI_27_1);

    const pi = await service.removeLink('1', 'l1');

    expect(repository.removeLink).toHaveBeenCalledWith('1', 'l1');
    expect(pi).toEqual(PI_27_1);
    expect(service.error()).toBeUndefined();
  });

  it('liefert kein PI und zeigt die Meldung des Servers, wenn es den Link nicht gibt (US-8)', async () => {
    repository.removeLink.mockRejectedValue(
      new HttpErrorResponse({ status: 404, error: { message: 'Den Link gibt es nicht.' } }),
    );

    const pi = await service.removeLink('1', 'unbekannt');

    expect(pi).toBeUndefined();
    expect(service.error()).toBe('Den Link gibt es nicht.');
  });
});
