import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Feature } from '../../core/models/feature.model';
import { Iteration } from '../../core/models/program-increment.model';
import { Team } from '../../core/models/team.model';
import { BoardCardComponent, SchedulingChoice } from './board-card.component';

/** PI 27.1 mit Iterationen zu 2 Wochen: fünf Entwicklungs-Iterationen und die IP als Nummer 6 (US-5). */
const ITERATIONS: Iteration[] = [
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

const TEAMS: Team[] = [
  { id: 'a', name: 'Adler' },
  { id: 'z', name: 'Zebra' },
];

const PORTAL: Feature = {
  id: 'f1',
  programIncrementId: 'pi-7',
  title: 'Portal',
  description: '',
  owner: '',
  wsjfRank: 2,
  estimate: 8,
  scheduled: false,
};
const PORTAL_SCHEDULED: Feature = { ...PORTAL, scheduled: true, teamId: 'a', iterationNumber: 2 };

/**
 * Vertrag mit der Vorlage (US-13): `app-board-card` zeigt Titel, WSJF-Rang und Schätzung in
 * `[data-testid="feature-card"]` (nur diese Angaben, US-12) und daneben einen nativen `<button>` «Einplanen …».
 * Er klappt das Inline-Formular `[data-testid="schedule-form"]` auf: zwei native `<select>` mit Label «Team» und
 * «Iteration» (Wert der Option: Team-ID bzw. Iterationsnummer, Text: Name; eine Platzhalter-Option hat den Wert
 * ""), dazu die Buttons «Einplanen», «Abbrechen» und, nur bei eingeplanten Karten, «Zurück ins Backlog».
 */
describe('BoardCardComponent', () => {
  let fixture: ComponentFixture<BoardCardComponent>;
  let scheduled: SchedulingChoice[];
  let unscheduled: number;

  async function render(feature: Feature): Promise<HTMLElement> {
    TestBed.configureTestingModule({ imports: [BoardCardComponent] });
    fixture = TestBed.createComponent(BoardCardComponent);
    fixture.componentRef.setInput('feature', feature);
    fixture.componentRef.setInput('teams', TEAMS);
    fixture.componentRef.setInput('iterations', ITERATIONS);
    scheduled = [];
    unscheduled = 0;
    fixture.componentInstance.schedule.subscribe((choice) => scheduled.push(choice));
    fixture.componentInstance.unschedule.subscribe(() => unscheduled++);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  function findButton(scope: Element, label: string): HTMLButtonElement | undefined {
    return Array.from(scope.querySelectorAll('button')).find(
      (b) => (b.textContent ?? '').replace(/\s+/g, ' ').trim() === label,
    );
  }

  function button(scope: Element, label: string): HTMLButtonElement {
    const found = findButton(scope, label);
    expect(found, `Button «${label}»`).toBeDefined();
    return found!;
  }

  async function click(target: HTMLElement): Promise<void> {
    target.click();
    await fixture.whenStable();
  }

  function form(el: HTMLElement): HTMLElement | null {
    return el.querySelector('[data-testid="schedule-form"]');
  }

  /** Das Auswahlfeld mit dem Label (über `<label>` oder `aria-label`). */
  function select(el: HTMLElement, label: string): HTMLSelectElement {
    const found = Array.from(el.querySelectorAll('select')).find((s) => {
      const labels = Array.from(s.labels ?? []).map((l) => l.textContent ?? '');
      return [...labels, s.getAttribute('aria-label') ?? ''].some((text) => text.includes(label));
    });
    expect(found, `Auswahl «${label}»`).toBeDefined();
    return found!;
  }

  /** Die Texte der wählbaren Optionen, ohne Platzhalter. */
  function options(field: HTMLSelectElement): string[] {
    return Array.from(field.options)
      .filter((o) => o.value !== '')
      .map((o) => (o.textContent ?? '').trim());
  }

  function choose(field: HTMLSelectElement, label: string): void {
    const option = Array.from(field.options).find((o) => (o.textContent ?? '').trim() === label);
    expect(option, `Option «${label}»`).toBeDefined();
    field.value = option!.value;
    field.dispatchEvent(new Event('change'));
    field.dispatchEvent(new Event('input'));
  }

  async function openForm(el: HTMLElement): Promise<HTMLElement> {
    await click(button(el, 'Einplanen …'));
    expect(form(el), 'Formular «Einplanen …»').not.toBeNull();
    return form(el)!;
  }

  it('zeigt die Aktion «Einplanen …», das Formular erst nach dem Klick (US-13)', async () => {
    const el = await render(PORTAL);

    expect(form(el)).toBeNull();

    await click(button(el, 'Einplanen …'));

    expect(form(el)).not.toBeNull();
  });

  it('bietet alle Teams zur Auswahl an (US-13)', async () => {
    const el = await render(PORTAL);

    const shown = await openForm(el);

    expect(options(select(shown, 'Team'))).toEqual(['Adler', 'Zebra']);
  });

  it('bietet nur die Entwicklungs-Iterationen an, nicht die IP-Iteration (US-13)', async () => {
    const el = await render(PORTAL);

    const shown = await openForm(el);

    expect(options(select(shown, 'Iteration'))).toEqual([
      'Iteration 1',
      'Iteration 2',
      'Iteration 3',
      'Iteration 4',
      'Iteration 5',
    ]);
  });

  it('meldet beim Einplanen das gewählte Team und die Nummer der Iteration (US-13)', async () => {
    const el = await render(PORTAL);
    const shown = await openForm(el);
    choose(select(shown, 'Team'), 'Zebra');
    choose(select(shown, 'Iteration'), 'Iteration 3');

    await click(button(shown, 'Einplanen'));

    expect(scheduled).toEqual([{ teamId: 'z', iterationNumber: 3 }]);
    expect(unscheduled).toBe(0);
  });

  it('meldet beim Verschieben einer eingeplanten Karte die neue Zelle (US-13)', async () => {
    const el = await render(PORTAL_SCHEDULED);
    const shown = await openForm(el);
    choose(select(shown, 'Team'), 'Zebra');
    choose(select(shown, 'Iteration'), 'Iteration 5');

    await click(button(shown, 'Einplanen'));

    expect(scheduled).toEqual([{ teamId: 'z', iterationNumber: 5 }]);
    expect(unscheduled).toBe(0);
  });

  it('schliesst das Formular bei «Abbrechen», ohne etwas zu melden (US-13)', async () => {
    const el = await render(PORTAL_SCHEDULED);
    const shown = await openForm(el);
    choose(select(shown, 'Team'), 'Zebra');
    choose(select(shown, 'Iteration'), 'Iteration 3');

    await click(button(shown, 'Abbrechen'));

    expect(form(el)).toBeNull();
    expect(scheduled).toEqual([]);
    expect(unscheduled).toBe(0);
  });

  it('bietet «Zurück ins Backlog» bei einer eingeplanten Karte an und meldet es (US-13)', async () => {
    const el = await render(PORTAL_SCHEDULED);
    const shown = await openForm(el);

    await click(button(shown, 'Zurück ins Backlog'));

    expect(unscheduled).toBe(1);
    expect(scheduled).toEqual([]);
  });

  it('bietet «Zurück ins Backlog» bei einer Karte im Backlog nicht an (US-13)', async () => {
    const el = await render(PORTAL);

    await openForm(el);

    expect(findButton(el, 'Zurück ins Backlog')).toBeUndefined();
  });

  it('lässt sich ganz mit nativen, per Tastatur bedienbaren Elementen einplanen (US-13)', async () => {
    const el = await render(PORTAL_SCHEDULED);
    expect(button(el, 'Einplanen …').tagName).toBe('BUTTON');
    const shown = await openForm(el);

    expect(select(shown, 'Team').tagName).toBe('SELECT');
    expect(select(shown, 'Iteration').tagName).toBe('SELECT');
    for (const label of ['Einplanen', 'Abbrechen', 'Zurück ins Backlog']) {
      const found = button(shown, label);
      expect(found.disabled, `«${label}» bedienbar`).toBe(false);
      expect(found.tabIndex, `«${label}» per Tab erreichbar`).toBeGreaterThanOrEqual(0);
    }
  });

  it('zeigt auf der Karte nur Titel, WSJF-Rang und Schätzung, die Aktion daneben (US-13)', async () => {
    const el = await render(PORTAL);

    const card = el.querySelector('[data-testid="feature-card"]');
    expect(card, 'Karte').not.toBeNull();
    expect((card!.textContent ?? '').replace(/\s+/g, ' ').trim()).toBe('Portal WSJF 2 8 SP');
  });
});
