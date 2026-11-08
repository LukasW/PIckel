import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { App } from './app';
import { appConfig } from './app.config';
import { Board } from './core/models/board.model';
import { ProgramIncrement } from './core/models/program-increment.model';
import { BoardRepository } from './core/ports/board.repository';
import { CapacitiesRepository } from './core/ports/capacities.repository';
import { FeaturesRepository } from './core/ports/features.repository';
import { MilestonesRepository } from './core/ports/milestones.repository';
import { ProgramIncrementsRepository } from './core/ports/program-increments.repository';
import { TeamsRepository } from './core/ports/teams.repository';

/** Lesebreite der Listen- und Formularseiten (Delta US-12 → Entscheide). */
const READING_WIDTH = 'max-w-4xl';

const PI_7: ProgramIncrement = {
  id: 'pi-7',
  name: 'PI 27.1',
  startDate: '2027-01-04',
  endDate: '2027-03-26',
  iterationLengthWeeks: 2,
  iterations: [],
  businessContext: '',
  artObjectives: '',
  links: [],
};

const EMPTY_BOARD: Board = {
  teams: [],
  iterations: [],
  backlog: [],
  scheduled: [],
  milestones: [],
  capacities: [],
  dependencies: [],
};

/**
 * Die Breite steuert `App` über die Routendaten der aktiven Route (Delta US-12). Darum rendert der Test `App`
 * selbst mit den echten `routes` aus `appConfig`: Fehlt das Flag an der Board-Route, wird er rot. Alle Ports
 * sind gemockt, damit keine Seite ans Netz geht.
 */
describe('App', () => {
  let fixture: ComponentFixture<App>;
  let router: Router;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        ...appConfig.providers,
        {
          provide: ProgramIncrementsRepository,
          useValue: {
            list: vi.fn().mockResolvedValue([PI_7]),
            get: vi.fn().mockResolvedValue(PI_7),
            plan: vi.fn(),
            updateBusinessContext: vi.fn(),
            addLink: vi.fn(),
            removeLink: vi.fn(),
          },
        },
        { provide: BoardRepository, useValue: { get: vi.fn().mockResolvedValue(EMPTY_BOARD) } },
        {
          provide: FeaturesRepository,
          useValue: {
            list: vi.fn().mockResolvedValue([]),
            add: vi.fn(),
            update: vi.fn(),
            importCsv: vi.fn(),
            remove: vi.fn(),
          },
        },
        {
          provide: TeamsRepository,
          useValue: {
            list: vi.fn().mockResolvedValue([]),
            add: vi.fn(),
            rename: vi.fn(),
            remove: vi.fn(),
          },
        },
        {
          provide: CapacitiesRepository,
          useValue: { list: vi.fn().mockResolvedValue([]), set: vi.fn() },
        },
        {
          provide: MilestonesRepository,
          useValue: {
            list: vi.fn().mockResolvedValue([]),
            add: vi.fn(),
            update: vi.fn(),
            remove: vi.fn(),
          },
        },
      ],
    });
    fixture = TestBed.createComponent(App);
    router = TestBed.inject(Router);
    await settle();
  });

  async function open(url: string): Promise<void> {
    await router.navigateByUrl(url);
    await settle();
  }

  async function settle(): Promise<void> {
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function header(): HTMLElement {
    return fixture.nativeElement.querySelector('header nav');
  }

  function content(): HTMLElement {
    return fixture.nativeElement.querySelector('main');
  }

  /** Die Links der Kopfzeile mit sichtbarem Text und Ziel. */
  function headerLinks(): { text: string; href: string | null }[] {
    return Array.from(header().querySelectorAll('a')).map((a) => ({
      text: (a.textContent ?? '').trim(),
      href: a.getAttribute('href'),
    }));
  }

  function headerLink(text: string): HTMLAnchorElement {
    const found = Array.from(header().querySelectorAll('a')).filter(
      (a) => (a.textContent ?? '').trim() === text,
    );
    expect(found, `Link «${text}» in der Kopfzeile`).toHaveLength(1);
    return found[0];
  }

  // --- Delta US-1 (Issue #50): Kopfzeile verlinkt die PI-Übersicht ---

  it('verlinkt in der Kopfzeile «Program Increments» auf die PI-Übersicht, neben «Teams» (US-1)', async () => {
    await open('/teams');

    expect(headerLinks()).toEqual(
      expect.arrayContaining([
        { text: 'Program Increments', href: '/' },
        { text: 'Teams', href: '/teams' },
      ]),
    );
  });

  it('zeigt den Link «Program Increments» auf jeder Seite, auch auf Detailseite und Board (US-1)', async () => {
    for (const url of ['/', '/program-increments/pi-7', '/program-increments/pi-7/board']) {
      await open(url);

      expect(router.url).toBe(url);
      expect(headerLink('Program Increments').getAttribute('href'), `auf ${url}`).toBe('/');
    }
  });

  it('führt über «Program Increments» in der Kopfzeile zur Liste der PIs (US-1)', async () => {
    await open('/teams');
    expect(content().textContent).not.toContain('PI 27.1');

    headerLink('Program Increments').click();
    await settle();

    expect(router.url).toBe('/');
    expect(content().textContent).toContain('PI 27.1');
  });

  it('nutzt auf dem Board die volle Fensterbreite für Kopfzeile und Inhalt (US-12)', async () => {
    await open('/program-increments/pi-7/board');

    expect(router.url).toBe('/program-increments/pi-7/board');
    expect(header().classList).not.toContain(READING_WIDTH);
    expect(content().classList).not.toContain(READING_WIDTH);
  });

  it('begrenzt auf der PI-Liste Kopfzeile und Inhalt auf die Lesebreite (US-12)', async () => {
    await open('/');

    expect(router.url).toBe('/');
    expect(header().classList).toContain(READING_WIDTH);
    expect(content().classList).toContain(READING_WIDTH);
  });

  it('begrenzt nach dem Wechsel vom Board zurück wieder auf die Lesebreite (US-12)', async () => {
    await open('/program-increments/pi-7/board');
    expect(header().classList).not.toContain(READING_WIDTH);

    await open('/program-increments/pi-7');

    expect(router.url).toBe('/program-increments/pi-7');
    expect(header().classList).toContain(READING_WIDTH);
    expect(content().classList).toContain(READING_WIDTH);
  });
});
