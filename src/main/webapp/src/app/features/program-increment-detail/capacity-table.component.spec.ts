import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Capacity } from '../../core/models/capacity.model';
import { ProgramIncrement } from '../../core/models/program-increment.model';
import { Team } from '../../core/models/team.model';
import { CapacitiesRepository } from '../../core/ports/capacities.repository';
import { TeamsRepository } from '../../core/ports/teams.repository';
import { CapacityTableComponent } from './capacity-table.component';

/** PI 27.1 mit fünf Entwicklungs-Iterationen und der IP-Iteration als sechster (US-5). */
const PI_27_1: ProgramIncrement = {
  id: 'pi-7',
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

const DEVELOPMENT_ITERATIONS = [
  'Iteration 1',
  'Iteration 2',
  'Iteration 3',
  'Iteration 4',
  'Iteration 5',
];
const RAKETE: Team = { id: 'r', name: 'Rakete' };
const ZEBRA: Team = { id: 'z', name: 'Zebra' };
const NEGATIVE_REJECTED = 'Die Kapazität muss eine ganze Zahl ab 0 sein.';
const NO_TEAMS_HINT = 'Noch keine Teams. Erfasse zuerst die Teams.';

/** Volle Matrix wie vom Server: jede Zelle Team × Entwicklungs-Iteration, ohne Eintrag 0. */
function matrix(teams: Team[], entries: Capacity[] = []): Capacity[] {
  const cells: Capacity[] = [];
  for (const team of teams) {
    for (const iterationNumber of [1, 2, 3, 4, 5]) {
      const entry = entries.find(
        (c) => c.teamId === team.id && c.iterationNumber === iterationNumber,
      );
      cells.push(entry ?? { teamId: team.id, iterationNumber, points: 0 });
    }
  }
  return cells;
}

const RAKETE_1_40: Capacity = { teamId: 'r', iterationNumber: 1, points: 40 };

describe('CapacityTableComponent', () => {
  let teams: {
    list: ReturnType<typeof vi.fn>;
    add: ReturnType<typeof vi.fn>;
    rename: ReturnType<typeof vi.fn>;
    remove: ReturnType<typeof vi.fn>;
  };
  let capacities: { list: ReturnType<typeof vi.fn>; set: ReturnType<typeof vi.fn> };

  /** Rendert die Tabelle für ein PI; `teamList` und `capacityList` sind die Antworten des Servers. */
  async function render(
    teamList: Team[],
    capacityList: Capacity[] = matrix(teamList),
    pi: ProgramIncrement = PI_27_1,
  ): Promise<ComponentFixture<CapacityTableComponent>> {
    teams = {
      list: vi.fn().mockResolvedValue(teamList),
      add: vi.fn(),
      rename: vi.fn(),
      remove: vi.fn(),
    };
    capacities = { list: vi.fn().mockResolvedValue(capacityList), set: vi.fn() };
    TestBed.configureTestingModule({
      imports: [CapacityTableComponent],
      providers: [
        provideRouter([]),
        { provide: TeamsRepository, useValue: teams },
        { provide: CapacitiesRepository, useValue: capacities },
      ],
    });
    const fixture = TestBed.createComponent(CapacityTableComponent);
    fixture.componentRef.setInput('programIncrement', pi);
    await fixture.whenStable();
    return fixture;
  }

  function fields(el: HTMLElement): HTMLInputElement[] {
    return Array.from(el.querySelectorAll<HTMLInputElement>('input[aria-label^="Kapazität "]'));
  }

  function field(el: HTMLElement, team: string, iteration: string): HTMLInputElement {
    const found = el.querySelector<HTMLInputElement>(
      `input[aria-label="Kapazität ${team}, ${iteration}"]`,
    );
    expect(found, `Feld «Kapazität ${team}, ${iteration}»`).not.toBeNull();
    return found!;
  }

  /** Tippt einen Wert und verlässt das Feld; erst das Verlassen (change) speichert. */
  async function enter(
    fixture: ComponentFixture<CapacityTableComponent>,
    target: HTMLInputElement,
    value: string,
  ): Promise<void> {
    target.value = value;
    target.dispatchEvent(new Event('input'));
    target.dispatchEvent(new Event('change'));
    await fixture.whenStable();
  }

  it('zeigt je Team eine Zeile und je Entwicklungs-Iteration ein Zahlenfeld (US-7)', async () => {
    const el = (await render([RAKETE, ZEBRA])).nativeElement as HTMLElement;

    expect(fields(el)).toHaveLength(2 * DEVELOPMENT_ITERATIONS.length);
    for (const team of ['Rakete', 'Zebra']) {
      for (const iteration of DEVELOPMENT_ITERATIONS) {
        expect(field(el, team, iteration).type).toBe('number');
      }
    }
    const rowTexts = Array.from(el.querySelectorAll('tbody tr')).map((r) => r.textContent ?? '');
    expect(rowTexts).toEqual([expect.stringContaining('Rakete'), expect.stringContaining('Zebra')]);
  });

  it('zeigt für die IP-Iteration keine Spalte (US-7)', async () => {
    const el = (await render([RAKETE])).nativeElement as HTMLElement;

    const headers = Array.from(el.querySelectorAll('thead th')).map((th) =>
      (th.textContent ?? '').trim(),
    );
    for (const iteration of DEVELOPMENT_ITERATIONS) {
      expect(headers).toContain(iteration);
    }
    expect(headers).not.toContain('IP');
    expect(el.querySelector('input[aria-label="Kapazität Rakete, IP"]')).toBeNull();
  });

  it('lädt die Teams und die Kapazitäten des angezeigten PI (US-7)', async () => {
    await render([RAKETE], matrix([RAKETE]), { ...PI_27_1, id: 'pi-8' });

    expect(teams.list).toHaveBeenCalled();
    expect(capacities.list).toHaveBeenCalledWith('pi-8');
  });

  it('zeigt die erfasste Kapazität und 0 in den Zellen ohne Eintrag (US-7)', async () => {
    const fixture = await render([RAKETE], matrix([RAKETE], [RAKETE_1_40]));
    const el = fixture.nativeElement as HTMLElement;

    expect(field(el, 'Rakete', 'Iteration 1').value).toBe('40');
    expect(field(el, 'Rakete', 'Iteration 2').value).toBe('0');
  });

  it('zeigt 0, wenn der Server für eine Zelle nichts liefert (US-7)', async () => {
    const fixture = await render([RAKETE], [RAKETE_1_40]);
    const el = fixture.nativeElement as HTMLElement;

    expect(field(el, 'Rakete', 'Iteration 1').value).toBe('40');
    expect(field(el, 'Rakete', 'Iteration 3').value).toBe('0');
  });

  it('speichert die Kapazität beim Verlassen des Feldes für Team und Iteration der Zelle (US-7)', async () => {
    const fixture = await render([RAKETE, ZEBRA]);
    const saved: Capacity = { teamId: 'z', iterationNumber: 3, points: 35 };
    capacities.set.mockResolvedValue(saved);
    capacities.list.mockResolvedValue(matrix([RAKETE, ZEBRA], [saved]));
    const el = fixture.nativeElement as HTMLElement;

    await enter(fixture, field(el, 'Zebra', 'Iteration 3'), '35');

    expect(capacities.set).toHaveBeenCalledTimes(1);
    expect(capacities.set).toHaveBeenCalledWith('pi-7', 'z', 3, 35);
    expect(field(el, 'Zebra', 'Iteration 3').value).toBe('35');
    expect(el.querySelector('[role="alert"]')).toBeNull();
  });

  it('speichert nicht schon beim Tippen (US-7)', async () => {
    const fixture = await render([RAKETE]);
    const target = field(fixture.nativeElement as HTMLElement, 'Rakete', 'Iteration 1');

    target.value = '12';
    target.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    expect(capacities.set).not.toHaveBeenCalled();
  });

  it('zeigt die Meldung als Alarm, wenn eine negative Kapazität abgelehnt wird (US-7)', async () => {
    const fixture = await render([RAKETE]);
    capacities.set.mockRejectedValue(
      new HttpErrorResponse({ status: 400, error: { message: NEGATIVE_REJECTED } }),
    );
    const el = fixture.nativeElement as HTMLElement;

    await enter(fixture, field(el, 'Rakete', 'Iteration 1'), '-5');

    expect(el.querySelector('[role="alert"]')?.textContent ?? '').toContain(NEGATIVE_REJECTED);
    expect(field(el, 'Rakete', 'Iteration 1').value).toBe('0');
  });

  it('lehnt eine Kommazahl schon im Formular mit Meldung ab (US-7)', async () => {
    const fixture = await render([RAKETE]);
    const el = fixture.nativeElement as HTMLElement;

    await enter(fixture, field(el, 'Rakete', 'Iteration 1'), '1.5');

    expect(capacities.set).not.toHaveBeenCalled();
    expect(el.querySelector('[role="alert"]')?.textContent ?? '').toContain(NEGATIVE_REJECTED);
    expect(field(el, 'Rakete', 'Iteration 1').value).toBe('0');
  });

  it('lehnt eine negative Zahl schon im Formular ab (US-7)', async () => {
    const fixture = await render([RAKETE]);
    const el = fixture.nativeElement as HTMLElement;

    await enter(fixture, field(el, 'Rakete', 'Iteration 1'), '-1');

    expect(capacities.set).not.toHaveBeenCalled();
    expect(el.querySelector('[role="alert"]')?.textContent ?? '').toContain(NEGATIVE_REJECTED);
  });

  it('vergisst die Formularmeldung, sobald ein gültiger Wert gespeichert ist (US-7)', async () => {
    const fixture = await render([RAKETE]);
    capacities.set.mockResolvedValue({ teamId: 'r', iterationNumber: 1, points: 8 });
    const el = fixture.nativeElement as HTMLElement;
    await enter(fixture, field(el, 'Rakete', 'Iteration 1'), '1.5');

    await enter(fixture, field(el, 'Rakete', 'Iteration 1'), '8');

    expect(el.querySelector('[role="alert"]')).toBeNull();
  });

  it('speichert ein geleertes Feld nicht und zeigt wieder den gespeicherten Wert (US-7)', async () => {
    const fixture = await render([RAKETE], matrix([RAKETE], [RAKETE_1_40]));
    const el = fixture.nativeElement as HTMLElement;

    await enter(fixture, field(el, 'Rakete', 'Iteration 1'), '');

    expect(capacities.set).not.toHaveBeenCalled();
    expect(field(el, 'Rakete', 'Iteration 1').value).toBe('40');
  });

  it('zeigt ohne Teams einen Hinweis mit Link zur Teamseite (US-7)', async () => {
    const el = (await render([], [])).nativeElement as HTMLElement;

    expect(el.textContent).toContain(NO_TEAMS_HINT);
    expect(el.querySelector('a[href="/teams"]'), 'Link zur Teamseite').not.toBeNull();
    expect(fields(el)).toHaveLength(0);
  });

  it('zeigt mit Teams keinen Hinweis, sondern die Tabelle (US-7)', async () => {
    const el = (await render([RAKETE])).nativeElement as HTMLElement;

    expect(el.textContent).not.toContain('Noch keine Teams');
    expect(field(el, 'Rakete', 'Iteration 1').value).toBe('0');
  });
});
