import { CdkDrag, CdkDragDrop, CdkDropList } from '@angular/cdk/drag-drop';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { appConfig } from '../../app.config';
import { Board } from '../../core/models/board.model';
import { Feature } from '../../core/models/feature.model';
import { BoardMilestone } from '../../core/models/milestone.model';
import { Team } from '../../core/models/team.model';
import { BoardRepository } from '../../core/ports/board.repository';
import { FeaturesRepository } from '../../core/ports/features.repository';
import { BoardService } from '../../core/services/board.service';
import { ProgramBoardComponent } from './program-board.component';

/** PI 27.1 (04.01.–26.03.2027) mit Iterationen zu 2 Wochen: fünf Entwicklungs-Iterationen und die IP (US-5). */
const ITERATIONS = [
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
}));

const ITERATION_NAMES = ITERATIONS.map((iteration) => iteration.name);
const ADLER: Team = { id: 'a', name: 'Adler' };
const ZEBRA: Team = { id: 'z', name: 'Zebra' };
const NO_TEAMS_HINT = 'Noch keine Teams. Erfasse zuerst die Teams des ART.';

function feature(id: string, title: string, wsjfRank?: number, estimate?: number): Feature {
  return {
    id,
    programIncrementId: 'pi-7',
    title,
    description: '',
    owner: '',
    wsjfRank,
    estimate,
    scheduled: false,
  };
}

// Titel ohne Ziffern, Rang und Schätzung verschieden: So zeigt der Text der Karte eindeutig, was angezeigt ist.
const PORTAL = feature('f1', 'Portal', 2, 8);
const LOGIN = feature('f2', 'Login', 5, 13);

function board(
  teams: Team[],
  backlog: Feature[] = [PORTAL, LOGIN],
  milestones: BoardMilestone[] = [],
): Board {
  return { teams, iterations: ITERATIONS, backlog, scheduled: [], milestones, capacities: [] };
}

// Milestones in PI 27.1 (US-15); `iterationNumber` liefert der Server.
const KICKOFF: BoardMilestone = {
  id: 'm1',
  name: 'Kickoff',
  date: '2027-01-05',
  iterationNumber: 1,
};
const RELEASE: BoardMilestone = {
  id: 'm2',
  name: 'Release',
  date: '2027-02-10',
  iterationNumber: 3,
};
const MESSE: BoardMilestone = { id: 'm3', name: 'Messe', date: '2027-02-12', iterationNumber: 3 };
const AUDIT: BoardMilestone = { id: 'm4', name: 'Audit', date: '2027-03-20', iterationNumber: 6 };

/**
 * Vertrag mit der Vorlage: Das Raster ist eine Tabelle `[data-testid="board-grid"]` mit den Iterationen als
 * Spaltenköpfe (`thead th`) und einer Zeile je Team (`tbody tr`); die Backlog-Spalte ist
 * `[data-testid="board-backlog"]` mit einer Karte `[data-testid="feature-card"]` je Feature.
 */
describe('ProgramBoardComponent', () => {
  let repository: { get: ReturnType<typeof vi.fn> };

  async function render(answer: Board, id = 'pi-7'): Promise<HTMLElement> {
    repository = { get: vi.fn().mockResolvedValue(answer) };
    TestBed.configureTestingModule({
      imports: [ProgramBoardComponent],
      providers: [
        provideRouter([]),
        { provide: BoardRepository, useValue: repository },
        { provide: FeaturesRepository, useValue: {} },
      ],
    });
    const fixture: ComponentFixture<ProgramBoardComponent> =
      TestBed.createComponent(ProgramBoardComponent);
    fixture.componentRef.setInput('id', id);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  function grid(el: HTMLElement): HTMLElement | null {
    return el.querySelector('[data-testid="board-grid"]');
  }

  function headers(el: HTMLElement): string[] {
    return Array.from(grid(el)?.querySelectorAll('thead th') ?? []).map((th) =>
      (th.textContent ?? '').replace(/\s+/g, ' ').trim(),
    );
  }

  /** Der Spaltenkopf, dessen Text mit dem Namen der Iteration beginnt. */
  function header(el: HTMLElement, name: string): string {
    const found = headers(el).filter((text) => text.startsWith(name));
    expect(found, `Spaltenkopf «${name}»`).toHaveLength(1);
    return found[0];
  }

  function rows(el: HTMLElement): string[] {
    return Array.from(grid(el)?.querySelectorAll('tbody tr') ?? []).map((tr) =>
      (tr.textContent ?? '').replace(/\s+/g, ' ').trim(),
    );
  }

  function cards(el: HTMLElement): string[] {
    return Array.from(
      el.querySelectorAll('[data-testid="board-backlog"] [data-testid="feature-card"]'),
    ).map((card) => (card.textContent ?? '').replace(/\s+/g, ' ').trim());
  }

  // --- Laden ---

  it('lädt das Board des PI aus dem Input (US-12)', async () => {
    await render(board([ADLER]), 'pi-8');

    expect(repository.get).toHaveBeenCalledWith('pi-8');
  });

  it('öffnet das Board über die Route program-increments/:id/board (US-12)', async () => {
    repository = { get: vi.fn().mockResolvedValue(board([ADLER])) };
    TestBed.configureTestingModule({
      providers: [...appConfig.providers, { provide: BoardRepository, useValue: repository }],
    });
    const harness = await RouterTestingHarness.create();

    const shown = await harness.navigateByUrl(
      '/program-increments/42/board',
      ProgramBoardComponent,
    );
    await harness.fixture.whenStable();

    expect(shown).toBeInstanceOf(ProgramBoardComponent);
    expect(repository.get).toHaveBeenCalledWith('42');
  });

  // --- AK 1: Raster ---

  it('zeigt eine Zeile je Team in alphabetischer Reihenfolge (US-12)', async () => {
    const el = await render(board([ADLER, ZEBRA]));

    expect(rows(el)).toEqual([expect.stringContaining('Adler'), expect.stringContaining('Zebra')]);
  });

  it('zeigt eine Spalte je Iteration in zeitlicher Reihenfolge, die IP zuletzt (US-12)', async () => {
    const el = await render(board([ADLER]));

    const iterationHeaders = headers(el).filter((text) =>
      ITERATION_NAMES.some((name) => text.startsWith(name)),
    );
    expect(iterationHeaders).toHaveLength(ITERATIONS.length);
    iterationHeaders.forEach((text, i) => expect(text.startsWith(ITERATION_NAMES[i])).toBe(true));
  });

  it('markiert die Spalte der IP-Iteration als IP-Iteration (US-12)', async () => {
    const el = await render(board([ADLER]));

    expect(header(el, 'IP')).toContain('IP-Iteration');
    for (const name of ITERATION_NAMES.slice(0, 5)) {
      expect(header(el, name)).not.toContain('IP-Iteration');
    }
  });

  // --- AK 2: Spaltenköpfe ---

  it('zeigt im Spaltenkopf Name und Zeitraum der Iteration als TT.MM. (US-12)', async () => {
    const el = await render(board([ADLER]));

    const first = header(el, 'Iteration 1');
    expect(first).toContain('04.01.');
    expect(first).toContain('17.01.');
    const ip = header(el, 'IP');
    expect(ip).toContain('15.03.');
    expect(ip).toContain('26.03.');
    expect(headers(el).join(' ')).not.toContain('2027');
  });

  // --- AK 3: Backlog-Spalte ---

  it('zeigt das Backlog als Karten mit Titel, WSJF-Rang und Schätzung in Backlog-Reihenfolge (US-12)', async () => {
    const el = await render(board([ADLER], [PORTAL, LOGIN]));

    const shown = cards(el);
    expect(shown).toHaveLength(2);
    expect(shown[0]).toContain('Portal');
    expect(shown[0]).toContain('WSJF 2');
    expect(shown[0]).toContain('8 SP');
    expect(shown[1]).toContain('Login');
    expect(shown[1]).toContain('WSJF 5');
    expect(shown[1]).toContain('13 SP');
  });

  // Annahme im Nachtlauf (notes.md US-12): fehlender Rang wird weggelassen. Statt einer fehlenden Schätzung steht
  // seit US-14 (Delta, AK 5) der Hinweis «ohne Schätzung».
  it('lässt auf der Karte fehlenden Rang weg und zeigt statt fehlender Schätzung «ohne Schätzung» (US-12, US-14)', async () => {
    const el = await render(board([ADLER], [feature('f3', 'Suche')]));

    expect(cards(el)).toEqual(['Suche ohne Schätzung']);
  });

  it('übernimmt die Reihenfolge des Backlogs vom Server (US-12)', async () => {
    const el = await render(board([ADLER], [LOGIN, PORTAL]));

    expect(cards(el)).toEqual([
      expect.stringContaining('Login'),
      expect.stringContaining('Portal'),
    ]);
  });

  it('zeigt die Backlog-Spalte links neben dem Raster (US-12)', async () => {
    const el = await render(board([ADLER]));

    const backlog = el.querySelector('[data-testid="board-backlog"]');
    const raster = grid(el);
    expect(backlog, 'Backlog-Spalte').not.toBeNull();
    expect(raster, 'Raster').not.toBeNull();
    expect(
      backlog!.compareDocumentPosition(raster!) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
  });

  // --- US-15: Milestones ---

  /** Das Element im Spaltenkopf, dessen Text genau der Name der Iteration ist. */
  function iterationName(el: HTMLElement, name: string): HTMLElement {
    const found = Array.from(grid(el)?.querySelectorAll<HTMLElement>('thead th *') ?? []).filter(
      (node) => (node.textContent ?? '').trim() === name,
    );
    expect(found.length, `Name «${name}» im Spaltenkopf`).toBeGreaterThan(0);
    return found[0];
  }

  /** Die Marker `[data-testid="milestone-marker"]` im Spaltenkopf der Iteration, als Text. */
  function markersOver(el: HTMLElement, name: string): string[] {
    const th = iterationName(el, name).closest('th')!;
    return Array.from(th.querySelectorAll('[data-testid="milestone-marker"]')).map((m) =>
      (m.textContent ?? '').replace(/\s+/g, ' ').trim(),
    );
  }

  function allMarkers(el: HTMLElement): Element[] {
    return Array.from(el.querySelectorAll('[data-testid="milestone-marker"]'));
  }

  it('zeigt einen Milestone als Marker mit Name und Datum TT.MM. im Kopf seiner Iteration (US-15)', async () => {
    const el = await render(board([ADLER], [], [RELEASE]));

    const shown = markersOver(el, 'Iteration 3');
    expect(shown).toHaveLength(1);
    expect(shown[0]).toContain('Release');
    expect(shown[0]).toContain('10.02.');
    expect(shown[0]).not.toContain('2027');
  });

  it('zeigt den Marker über dem Namen der Iteration (US-15)', async () => {
    const el = await render(board([ADLER], [], [RELEASE]));

    const marker = allMarkers(el)[0];
    expect(marker, 'Marker').toBeDefined();
    expect(
      marker.compareDocumentPosition(iterationName(el, 'Iteration 3')) &
        Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
  });

  it('zeigt über Iterationen ohne Milestone keinen Marker (US-15)', async () => {
    const el = await render(board([ADLER], [], [RELEASE]));

    expect(allMarkers(el)).toHaveLength(1);
    for (const name of ITERATION_NAMES.filter((n) => n !== 'Iteration 3')) {
      expect(markersOver(el, name), `Iteration «${name}»`).toEqual([]);
    }
  });

  it('stellt jeden Marker über die Iteration mit seiner Nummer, auch über die IP (US-15)', async () => {
    const el = await render(board([ADLER], [], [KICKOFF, RELEASE, AUDIT]));

    expect(markersOver(el, 'Iteration 1')).toEqual([expect.stringContaining('Kickoff')]);
    expect(markersOver(el, 'Iteration 3')).toEqual([expect.stringContaining('Release')]);
    expect(markersOver(el, 'IP')).toEqual([expect.stringContaining('Audit')]);
    expect(markersOver(el, 'IP')[0]).toContain('20.03.');
  });

  it('zeigt mehrere Milestones einer Iteration alle, in der Reihenfolge des Servers (US-15)', async () => {
    const el = await render(board([ADLER], [], [KICKOFF, RELEASE, MESSE]));

    const shown = markersOver(el, 'Iteration 3');
    expect(shown).toHaveLength(2);
    expect(shown[0]).toContain('Release');
    expect(shown[0]).toContain('10.02.');
    expect(shown[1]).toContain('Messe');
    expect(shown[1]).toContain('12.02.');
    expect(markersOver(el, 'Iteration 1')).toEqual([expect.stringContaining('Kickoff')]);
  });

  it('zeigt ohne Milestones keinen Marker (US-15)', async () => {
    const el = await render(board([ADLER]));

    expect(grid(el), 'Raster').not.toBeNull();
    expect(allMarkers(el)).toHaveLength(0);
  });

  // --- AK 4: ohne Teams ---

  it('zeigt ohne Teams einen Hinweis mit Link zur Teamseite statt eines Rasters (US-12)', async () => {
    const el = await render(board([]));

    expect(el.textContent).toContain(NO_TEAMS_HINT);
    expect(el.querySelector('a[href="/teams"]'), 'Link zur Teamseite').not.toBeNull();
    expect(grid(el), 'Raster').toBeNull();
  });

  it('zeigt mit Teams keinen Hinweis, sondern das Raster (US-12)', async () => {
    const el = await render(board([ADLER]));

    expect(el.textContent).not.toContain('Noch keine Teams');
    expect(rows(el)).toEqual([expect.stringContaining('Adler')]);
  });

  // --- US-13: Einplanen ---

  /**
   * Vertrag mit der Vorlage (US-13): Die Zellen `tbody tr td` stehen in der Reihenfolge der Iterationen. Jede Karte
   * ist ein `app-board-card` mit `cdkDrag` und `[cdkDragData]="feature"`. Das Backlog und jede Zelle einer
   * Entwicklungs-Iteration sind (oder enthalten) eine `cdkDropList`, die IP-Zellen nicht; das Ablegen läuft über
   * `(cdkDropListDropped)`. Eine gescheiterte Aktion (`actionError`) steht als `[role="alert"]` über dem Board.
   */
  describe('Einplanen (US-13)', () => {
    let fixture: ComponentFixture<ProgramBoardComponent>;
    let service: BoardService;
    let schedule: ReturnType<typeof vi.fn>;
    let unschedule: ReturnType<typeof vi.fn>;

    const SUCHE: Feature = {
      ...feature('f3', 'Suche', 3, 5),
      scheduled: true,
      teamId: 'a',
      iterationNumber: 2,
    };
    const KASSE: Feature = {
      ...feature('f4', 'Kasse', 4, 3),
      scheduled: true,
      teamId: 'a',
      iterationNumber: 2,
    };
    const ARCHIV: Feature = {
      ...feature('f5', 'Archiv', 6, 2),
      scheduled: true,
      teamId: 'z',
      iterationNumber: 4,
    };

    function planned(scheduled: Feature[], backlog: Feature[] = [PORTAL, LOGIN]): Board {
      return {
        teams: [ADLER, ZEBRA],
        iterations: ITERATIONS,
        backlog,
        scheduled,
        milestones: [],
        capacities: [],
      };
    }

    async function show(answer: Board): Promise<HTMLElement> {
      repository = { get: vi.fn().mockResolvedValue(answer) };
      TestBed.configureTestingModule({
        imports: [ProgramBoardComponent],
        providers: [
          provideRouter([]),
          { provide: BoardRepository, useValue: repository },
          { provide: FeaturesRepository, useValue: { schedule: vi.fn(), unschedule: vi.fn() } },
        ],
      });
      service = TestBed.inject(BoardService);
      schedule = vi.spyOn(service, 'schedule').mockResolvedValue(undefined) as never;
      unschedule = vi.spyOn(service, 'unschedule').mockResolvedValue(undefined) as never;
      fixture = TestBed.createComponent(ProgramBoardComponent);
      fixture.componentRef.setInput('id', 'pi-7');
      await fixture.whenStable();
      return fixture.nativeElement as HTMLElement;
    }

    function teamRow(el: HTMLElement, team: string): HTMLElement {
      const found = Array.from(grid(el)?.querySelectorAll<HTMLElement>('tbody tr') ?? []).find(
        (tr) => (tr.querySelector('th')?.textContent ?? '').trim() === team,
      );
      expect(found, `Zeile ${team}`).toBeDefined();
      return found!;
    }

    /** Die Zelle Team × Iteration (Nummer 1…6). */
    function cell(el: HTMLElement, team: string, iterationNumber: number): HTMLElement {
      const cells = Array.from(teamRow(el, team).querySelectorAll<HTMLElement>('td'));
      expect(cells, `Zellen von ${team}`).toHaveLength(ITERATIONS.length);
      return cells[iterationNumber - 1];
    }

    function titlesIn(scope: Element): string[] {
      return Array.from(scope.querySelectorAll('[data-testid="feature-card"]')).map((card) =>
        (card.textContent ?? '').replace(/\s+/g, ' ').trim(),
      );
    }

    function isDropZone(scope: HTMLElement): boolean {
      return (
        fixture.debugElement
          .queryAll(By.directive(CdkDropList))
          .filter((d) => scope === d.nativeElement || scope.contains(d.nativeElement)).length > 0
      );
    }

    function dropList(scope: HTMLElement): CdkDropList {
      const found = fixture.debugElement
        .queryAll(By.directive(CdkDropList))
        .find((d) => scope === d.nativeElement || scope.contains(d.nativeElement));
      expect(found, 'Drop-Zone').toBeDefined();
      return found!.injector.get(CdkDropList);
    }

    function dragOf(title: string): CdkDrag {
      const found = fixture.debugElement
        .queryAll(By.directive(CdkDrag))
        .find((d) => ((d.nativeElement as HTMLElement).textContent ?? '').includes(title));
      expect(found, `Ziehbare Karte «${title}»`).toBeDefined();
      return found!.injector.get(CdkDrag);
    }

    /** Löst das Ablegen aus, wie es CDK nach dem Ziehen tut; jsdom kann nicht wirklich ziehen. */
    async function drop(title: string, from: HTMLElement, to: HTMLElement): Promise<void> {
      const source = dropList(from);
      const target = dropList(to);
      const event: CdkDragDrop<unknown, unknown, unknown> = {
        previousIndex: 0,
        currentIndex: 0,
        item: dragOf(title),
        container: target,
        previousContainer: source,
        isPointerOverContainer: true,
        distance: { x: 10, y: 10 },
        dropPoint: { x: 10, y: 10 },
        event: new MouseEvent('mouseup'),
      };
      target.dropped.emit(event);
      await fixture.whenStable();
    }

    function backlog(el: HTMLElement): HTMLElement {
      return el.querySelector<HTMLElement>('[data-testid="board-backlog"]')!;
    }

    it('zeigt eine eingeplante Karte in ihrer Zelle und nicht im Backlog (US-13)', async () => {
      const el = await show(planned([SUCHE]));

      expect(titlesIn(cell(el, 'Adler', 2))).toEqual([expect.stringContaining('Suche')]);
      expect(cards(el)).toEqual([
        expect.stringContaining('Portal'),
        expect.stringContaining('Login'),
      ]);
    });

    it('zeigt eine eingeplante Karte in keiner anderen Zelle (US-13)', async () => {
      const el = await show(planned([SUCHE]));

      for (const team of ['Adler', 'Zebra']) {
        for (const iteration of ITERATIONS) {
          if (team === 'Adler' && iteration.number === 2) continue;
          expect(titlesIn(cell(el, team, iteration.number)), `${team} × ${iteration.name}`).toEqual(
            [],
          );
        }
      }
    });

    it('zeigt die Karten einer Zelle in der Reihenfolge des Servers (US-13)', async () => {
      const el = await show(planned([KASSE, SUCHE, ARCHIV]));

      expect(titlesIn(cell(el, 'Adler', 2))).toEqual([
        expect.stringContaining('Kasse'),
        expect.stringContaining('Suche'),
      ]);
      expect(titlesIn(cell(el, 'Zebra', 4))).toEqual([expect.stringContaining('Archiv')]);
    });

    it('macht das Backlog und die Zellen der Entwicklungs-Iterationen zu Ablagen, die IP nicht (US-13)', async () => {
      const el = await show(planned([]));

      expect(isDropZone(backlog(el)), 'Backlog').toBe(true);
      for (const team of ['Adler', 'Zebra']) {
        for (const iteration of ITERATIONS) {
          expect(isDropZone(cell(el, team, iteration.number)), `${team} × ${iteration.name}`).toBe(
            !iteration.innovationAndPlanning,
          );
        }
      }
    });

    it('plant eine Karte ein, die aus dem Backlog in eine Zelle gezogen wird (US-13)', async () => {
      const el = await show(planned([]));

      await drop('Portal', backlog(el), cell(el, 'Zebra', 3));

      expect(schedule).toHaveBeenCalledWith(PORTAL, 'z', 3);
      expect(unschedule).not.toHaveBeenCalled();
    });

    it('verschiebt eine eingeplante Karte, die in eine andere Zelle gezogen wird (US-13)', async () => {
      const el = await show(planned([SUCHE]));

      await drop('Suche', cell(el, 'Adler', 2), cell(el, 'Zebra', 5));

      expect(schedule).toHaveBeenCalledWith(SUCHE, 'z', 5);
      expect(unschedule).not.toHaveBeenCalled();
    });

    it('nimmt eine eingeplante Karte zurück, die ins Backlog gezogen wird (US-13)', async () => {
      const el = await show(planned([SUCHE]));

      await drop('Suche', cell(el, 'Adler', 2), backlog(el));

      expect(unschedule).toHaveBeenCalledWith(SUCHE);
      expect(schedule).not.toHaveBeenCalled();
    });

    it('ruft nichts auf, wenn eine Karte in dieselbe Zelle oder ins Backlog zurückgelegt wird (US-13)', async () => {
      const el = await show(planned([SUCHE]));

      await drop('Suche', cell(el, 'Adler', 2), cell(el, 'Adler', 2));
      await drop('Portal', backlog(el), backlog(el));

      expect(schedule).not.toHaveBeenCalled();
      expect(unschedule).not.toHaveBeenCalled();
    });

    it('plant über die Aktion «Einplanen …» an der Karte ohne Maus ein (US-13)', async () => {
      const el = await show(planned([]));
      const card = Array.from(backlog(el).querySelectorAll<HTMLElement>('app-board-card')).find(
        (c) => (c.textContent ?? '').includes('Portal'),
      );
      expect(card, 'Karte «Portal» im Backlog').toBeDefined();
      buttonIn(card!, 'Einplanen …').click();
      await fixture.whenStable();

      pick(card!, 'Team', 'Zebra');
      pick(card!, 'Iteration', 'Iteration 3');
      buttonIn(card!, 'Einplanen').click();
      await fixture.whenStable();

      expect(schedule).toHaveBeenCalledWith(PORTAL, 'z', 3);
    });

    it('nimmt über die Aktion «Einplanen …» eine eingeplante Karte ohne Maus zurück (US-13)', async () => {
      const el = await show(planned([SUCHE]));
      const card = cell(el, 'Adler', 2).querySelector<HTMLElement>('app-board-card');
      expect(card, 'Karte «Suche» in der Zelle').not.toBeNull();
      buttonIn(card!, 'Einplanen …').click();
      await fixture.whenStable();

      buttonIn(card!, 'Zurück ins Backlog').click();
      await fixture.whenStable();

      expect(unschedule).toHaveBeenCalledWith(SUCHE);
    });

    it('zeigt die Meldung einer gescheiterten Aktion als Alarm über dem Board (US-13)', async () => {
      const el = await show(planned([SUCHE]));

      service.actionError.set('In der IP-Iteration wird keine Feature-Arbeit geplant.');
      await fixture.whenStable();

      const alert = el.querySelector('[role="alert"]');
      expect(alert?.textContent).toContain(
        'In der IP-Iteration wird keine Feature-Arbeit geplant.',
      );
      expect(
        alert!.compareDocumentPosition(grid(el)!) & Node.DOCUMENT_POSITION_FOLLOWING,
      ).toBeTruthy();
      expect(titlesIn(cell(el, 'Adler', 2))).toEqual([expect.stringContaining('Suche')]);
    });

    it('zeigt ohne gescheiterte Aktion keinen Alarm (US-13)', async () => {
      const el = await show(planned([SUCHE]));

      expect(el.querySelector('[role="alert"]')).toBeNull();
      expect(grid(el), 'Raster').not.toBeNull();
    });

    function buttonIn(scope: Element, label: string): HTMLButtonElement {
      const found = Array.from(scope.querySelectorAll('button')).find(
        (b) => (b.textContent ?? '').replace(/\s+/g, ' ').trim() === label,
      );
      expect(found, `Button «${label}»`).toBeDefined();
      return found!;
    }

    function pick(scope: Element, label: string, option: string): void {
      const field = Array.from(scope.querySelectorAll('select')).find((s) =>
        [
          ...Array.from(s.labels ?? []).map((l) => l.textContent ?? ''),
          s.getAttribute('aria-label') ?? '',
        ].some((text) => text.includes(label)),
      );
      expect(field, `Auswahl «${label}»`).toBeDefined();
      const found = Array.from(field!.options).find((o) => (o.textContent ?? '').trim() === option);
      expect(found, `Option «${option}»`).toBeDefined();
      field!.value = found!.value;
      field!.dispatchEvent(new Event('change'));
      field!.dispatchEvent(new Event('input'));
    }
  });

  // --- US-14: Kapazität pro Zelle ---

  /**
   * Vertrag mit der Vorlage (US-14): Jede Zelle `tbody tr td` einer Entwicklungs-Iteration zeigt oben
   * «geplant / verfügbar SP», auch leer; eine überlastete Zelle trägt den Text «Überlast» und ist mit `border-danger`
   * und `bg-danger/10` markiert (die Zelle selbst oder ein Element darin). Die IP-Spalte zeigt keine Kapazität.
   */
  describe('Kapazität pro Zelle (US-14)', () => {
    let fixture: ComponentFixture<ProgramBoardComponent>;
    let features: { schedule: ReturnType<typeof vi.fn>; unschedule: ReturnType<typeof vi.fn> };

    function placed(f: Feature, teamId: string, iterationNumber: number): Feature {
      return { ...f, scheduled: true, teamId, iterationNumber };
    }

    /** Wie der Server: jede Zelle Team × Entwicklungs-Iteration hat einen Eintrag, ohne Erfassung 0; die IP keinen. */
    function withCapacities(scheduled: Feature[], points: Record<string, number>): Board {
      const capacities = [ADLER, ZEBRA].flatMap((team) =>
        ITERATIONS.filter((i) => !i.innovationAndPlanning).map((i) => ({
          teamId: team.id,
          iterationNumber: i.number,
          points: points[`${team.id}|${i.number}`] ?? 0,
        })),
      );
      return {
        teams: [ADLER, ZEBRA],
        iterations: ITERATIONS,
        backlog: [],
        scheduled,
        milestones: [],
        capacities,
      };
    }

    async function show(answer: Board): Promise<HTMLElement> {
      repository = { get: vi.fn().mockResolvedValue(answer) };
      features = { schedule: vi.fn(), unschedule: vi.fn() };
      TestBed.configureTestingModule({
        imports: [ProgramBoardComponent],
        providers: [
          provideRouter([]),
          { provide: BoardRepository, useValue: repository },
          { provide: FeaturesRepository, useValue: features },
        ],
      });
      fixture = TestBed.createComponent(ProgramBoardComponent);
      fixture.componentRef.setInput('id', 'pi-7');
      await fixture.whenStable();
      return fixture.nativeElement as HTMLElement;
    }

    function cell(el: HTMLElement, team: string, iterationNumber: number): HTMLElement {
      const row = Array.from(grid(el)?.querySelectorAll<HTMLElement>('tbody tr') ?? []).find(
        (tr) => (tr.querySelector('th')?.textContent ?? '').trim() === team,
      );
      expect(row, `Zeile ${team}`).toBeDefined();
      const cells = Array.from(row!.querySelectorAll<HTMLElement>('td'));
      expect(cells, `Zellen von ${team}`).toHaveLength(ITERATIONS.length);
      return cells[iterationNumber - 1];
    }

    function text(scope: Element): string {
      return (scope.textContent ?? '').replace(/\s+/g, ' ').trim();
    }

    /** Die Anzeige «geplant / verfügbar SP» der Zelle, oder `undefined`, wenn sie fehlt. */
    function loadShown(scope: HTMLElement): string | undefined {
      const match = /(\d+)\s*\/\s*(\d+)\s*SP/.exec(text(scope));
      return match ? `${match[1]} / ${match[2]} SP` : undefined;
    }

    function hasClass(scope: HTMLElement, name: string): boolean {
      return [scope, ...Array.from(scope.querySelectorAll('*'))].some((e) =>
        e.classList.contains(name),
      );
    }

    function markedAsOverload(scope: HTMLElement): boolean {
      return hasClass(scope, 'border-danger') && hasClass(scope, 'bg-danger/10');
    }

    function expectNotOverloaded(scope: HTMLElement): void {
      expect(text(scope)).not.toContain('Überlast');
      expect(hasClass(scope, 'border-danger'), 'border-danger').toBe(false);
      expect(hasClass(scope, 'bg-danger/10'), 'bg-danger/10').toBe(false);
    }

    it('zeigt in der Zelle «geplant / verfügbar SP» und keine Überlast (US-14)', async () => {
      const el = await show(withCapacities([placed(LOGIN, 'a', 1)], { 'a|1': 20 }));

      expect(loadShown(cell(el, 'Adler', 1))).toBe('13 / 20 SP');
      expectNotOverloaded(cell(el, 'Adler', 1));
    });

    it('zeigt die Anzeige in jeder Zelle einer Entwicklungs-Iteration, auch leer (US-14)', async () => {
      const el = await show(withCapacities([placed(LOGIN, 'a', 1)], { 'a|1': 20, 'z|3': 10 }));

      expect(loadShown(cell(el, 'Zebra', 3))).toBe('0 / 10 SP');
      for (const team of ['Adler', 'Zebra']) {
        for (const iteration of ITERATIONS.filter((i) => !i.innovationAndPlanning)) {
          if (team === 'Adler' && iteration.number === 1) continue;
          if (team === 'Zebra' && iteration.number === 3) continue;
          expect(loadShown(cell(el, team, iteration.number)), `${team} × ${iteration.name}`).toBe(
            '0 / 0 SP',
          );
        }
      }
    });

    it('zeigt die Anzeige oben in der Zelle, vor den Karten (US-14)', async () => {
      const el = await show(withCapacities([placed(LOGIN, 'a', 1)], { 'a|1': 20 }));

      const shown = cell(el, 'Adler', 1);
      expect(text(shown).indexOf('13 / 20 SP'), 'Anzeige').toBeGreaterThanOrEqual(0);
      expect(text(shown).indexOf('13 / 20 SP')).toBeLessThan(text(shown).indexOf('Login'));
    });

    it('zeigt in der IP-Spalte keine Kapazität (US-14)', async () => {
      const el = await show(withCapacities([placed(LOGIN, 'a', 1)], { 'a|1': 20 }));

      for (const team of ['Adler', 'Zebra']) {
        const ip = cell(el, team, 6);
        expect(loadShown(ip), `${team} × IP`).toBeUndefined();
        expect(text(ip), `${team} × IP`).not.toContain('SP');
        expect(text(ip), `${team} × IP`).not.toContain('Überlast');
      }
    });

    it('hebt eine Zelle über der Kapazität rot hervor und beschriftet sie mit «Überlast» (US-14)', async () => {
      // Login 13 + Suche 8 = 21 > 20
      const suche = feature('f3', 'Suche', 3, 8);
      const el = await show(
        withCapacities([placed(LOGIN, 'a', 1), placed(suche, 'a', 1)], { 'a|1': 20, 'z|1': 20 }),
      );

      const shown = cell(el, 'Adler', 1);
      expect(loadShown(shown)).toBe('21 / 20 SP');
      expect(text(shown)).toContain('Überlast');
      expect(markedAsOverload(shown), 'rot markiert').toBe(true);
      expectNotOverloaded(cell(el, 'Zebra', 1));
    });

    it('markiert eine genau ausgelastete Zelle nicht als Überlast (US-14)', async () => {
      const el = await show(withCapacities([placed(LOGIN, 'a', 1)], { 'a|1': 13 }));

      expect(loadShown(cell(el, 'Adler', 1))).toBe('13 / 13 SP');
      expectNotOverloaded(cell(el, 'Adler', 1));
    });

    it('zählt ein Feature ohne Schätzung als 0 und zeigt auf der Karte «ohne Schätzung» (US-14)', async () => {
      const ohne = feature('f2', 'Login', 5);
      const el = await show(withCapacities([placed(ohne, 'a', 1)], {}));

      const shown = cell(el, 'Adler', 1);
      expect(loadShown(shown)).toBe('0 / 0 SP');
      expectNotOverloaded(shown);
      const card = shown.querySelector<HTMLElement>('app-board-card');
      expect(card, 'Karte «Login» in der Zelle').not.toBeNull();
      expect(text(card!)).toContain('ohne Schätzung');
    });

    it('zeigt nach dem Verschieben die neuen Werte ohne Neuladen, nach einem Fehler wieder die alten (US-14)', async () => {
      const portal = placed(PORTAL, 'a', 1);
      const el = await show(withCapacities([portal], { 'a|1': 10, 'a|2': 10 }));
      let reject: (e: unknown) => void = () => undefined;
      features.schedule.mockReturnValue(new Promise((_, rej) => (reject = rej)));
      const service = TestBed.inject(BoardService);

      const moving = service.schedule(portal, 'a', 2);
      moving.catch(() => undefined);
      await fixture.whenStable();

      expect(loadShown(cell(el, 'Adler', 1))).toBe('0 / 10 SP');
      expect(loadShown(cell(el, 'Adler', 2))).toBe('8 / 10 SP');
      expect(repository.get).toHaveBeenCalledTimes(1);

      reject(new Error('offline'));
      await moving;
      await fixture.whenStable();

      expect(loadShown(cell(el, 'Adler', 1))).toBe('8 / 10 SP');
      expect(loadShown(cell(el, 'Adler', 2))).toBe('0 / 10 SP');
      expect(repository.get).toHaveBeenCalledTimes(1);
    });
  });
});
