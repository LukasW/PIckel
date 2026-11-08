import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { appConfig } from '../../app.config';
import { Board } from '../../core/models/board.model';
import { Feature } from '../../core/models/feature.model';
import { Team } from '../../core/models/team.model';
import { BoardRepository } from '../../core/ports/board.repository';
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

function board(teams: Team[], backlog: Feature[] = [PORTAL, LOGIN]): Board {
  return { teams, iterations: ITERATIONS, backlog };
}

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
      providers: [provideRouter([]), { provide: BoardRepository, useValue: repository }],
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

  // Annahme im Nachtlauf (notes.md): fehlender Rang oder fehlende Schätzung wird weggelassen.
  it('lässt auf der Karte fehlenden Rang und fehlende Schätzung weg (US-12)', async () => {
    const el = await render(board([ADLER], [feature('f3', 'Suche')]));

    expect(cards(el)).toEqual(['Suche']);
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
});
