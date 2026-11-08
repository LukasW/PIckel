import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Team } from '../../core/models/team.model';
import { TeamsRepository } from '../../core/ports/teams.repository';
import { TeamsComponent } from './teams.component';

const RAKETE: Team = { id: 'r', name: 'Rakete' };
const ZEBRA: Team = { id: 'z', name: 'Zebra' };

describe('TeamsComponent', () => {
  let repository: {
    list: ReturnType<typeof vi.fn>;
    add: ReturnType<typeof vi.fn>;
    rename: ReturnType<typeof vi.fn>;
    remove: ReturnType<typeof vi.fn>;
  };

  /** Rendert die Seite; `lists` sind die Antworten des Servers auf das erste, zweite, … Laden. */
  async function render(...lists: Team[][]): Promise<ComponentFixture<TeamsComponent>> {
    const list = vi.fn();
    for (const answer of lists) {
      list.mockResolvedValueOnce(answer);
    }
    list.mockResolvedValue(lists.at(-1) ?? []);
    repository = { list, add: vi.fn(), rename: vi.fn(), remove: vi.fn() };
    TestBed.configureTestingModule({
      imports: [TeamsComponent],
      providers: [provideRouter([]), { provide: TeamsRepository, useValue: repository }],
    });
    const fixture = TestBed.createComponent(TeamsComponent);
    await fixture.whenStable();
    return fixture;
  }

  function rows(el: HTMLElement): HTMLElement[] {
    return Array.from(el.querySelectorAll<HTMLElement>('[data-testid="team-row"]'));
  }

  function row(el: HTMLElement, name: string): HTMLElement {
    const found = rows(el).find((r) => r.textContent!.includes(name));
    expect(found, `Zeile von ${name}`).toBeDefined();
    return found!;
  }

  function button(scope: HTMLElement, label: string): HTMLButtonElement {
    const found = Array.from(scope.querySelectorAll('button')).find(
      (b) => b.textContent!.trim() === label,
    );
    expect(found, `Button «${label}»`).toBeDefined();
    return found!;
  }

  function hasButton(scope: HTMLElement, label: string): boolean {
    return Array.from(scope.querySelectorAll('button')).some(
      (b) => b.textContent!.trim() === label,
    );
  }

  async function click(fixture: ComponentFixture<TeamsComponent>, target: HTMLElement) {
    target.click();
    await fixture.whenStable();
  }

  function type(field: HTMLInputElement, value: string) {
    field.value = value;
    field.dispatchEvent(new Event('input'));
  }

  /** Das Formular zum Erfassen, erkennbar an seinem Button «Team erfassen». */
  function addForm(el: HTMLElement): HTMLFormElement {
    const form = Array.from(el.querySelectorAll('form')).find((f) => hasButton(f, 'Team erfassen'));
    expect(form, 'Formular «Team erfassen»').toBeDefined();
    return form!;
  }

  async function addTeam(fixture: ComponentFixture<TeamsComponent>, name: string) {
    const el = fixture.nativeElement as HTMLElement;
    const form = addForm(el);
    type(form.querySelector('input')!, name);
    await click(fixture, button(form, 'Team erfassen'));
    return el;
  }

  it('zeigt die Überschrift «Teams» (US-6)', async () => {
    const el = (await render([])).nativeElement as HTMLElement;

    expect(el.querySelector('h1')?.textContent?.trim()).toBe('Teams');
  });

  it('zeigt den Leerzustand ohne Teams (US-6)', async () => {
    const el = (await render([])).nativeElement as HTMLElement;

    expect(el.textContent).toContain('Noch keine Teams. Erfasse das erste Team.');
    expect(rows(el)).toHaveLength(0);
  });

  it('zeigt keinen Leerzustand, sobald es ein Team gibt (US-6)', async () => {
    const el = (await render([RAKETE])).nativeElement as HTMLElement;

    expect(el.textContent).not.toContain('Noch keine Teams');
  });

  it('listet die Teams in der Reihenfolge des Servers (US-6)', async () => {
    const el = (await render([RAKETE, ZEBRA])).nativeElement as HTMLElement;

    expect(rows(el).map((r) => r.textContent)).toEqual([
      expect.stringContaining('Rakete'),
      expect.stringContaining('Zebra'),
    ]);
  });

  it('erfasst ein Team, zeigt es in der Liste und leert das Feld (US-6)', async () => {
    const fixture = await render([], [RAKETE]);
    repository.add.mockResolvedValue(RAKETE);

    const el = await addTeam(fixture, 'Rakete');

    expect(repository.add).toHaveBeenCalledWith('Rakete');
    expect(rows(el).map((r) => r.textContent)).toEqual([expect.stringContaining('Rakete')]);
    expect(addForm(el).querySelector('input')!.value).toBe('');
  });

  it('lehnt einen Teamnamen aus Leerzeichen im Formular ab (US-6)', async () => {
    const el = await addTeam(await render([]), '   ');

    expect(addForm(el).textContent).toContain('Der Teamname darf nicht leer sein.');
    expect(repository.add).not.toHaveBeenCalled();
  });

  it('lehnt einen Teamnamen mit 51 Zeichen im Formular ab (US-6)', async () => {
    const el = await addTeam(await render([]), 'x'.repeat(51));

    expect(addForm(el).textContent).toContain('Der Teamname darf höchstens 50 Zeichen lang sein.');
    expect(repository.add).not.toHaveBeenCalled();
  });

  it('nimmt einen Teamnamen mit genau 50 Zeichen an (US-6)', async () => {
    const fixture = await render([]);
    const name = 'x'.repeat(50);
    repository.add.mockResolvedValue({ id: 'x', name });

    const el = await addTeam(fixture, name);

    expect(repository.add).toHaveBeenCalledWith(name);
    expect(addForm(el).textContent).not.toContain('höchstens');
  });

  it('misst die Länge des Teamnamens ohne Leerzeichen am Rand (US-6)', async () => {
    const fixture = await render([]);
    repository.add.mockResolvedValue({ id: 'x', name: 'x'.repeat(50) });

    const el = await addTeam(fixture, '  ' + 'x'.repeat(50) + '  ');

    expect(addForm(el).textContent).not.toContain('höchstens');
    expect(repository.add).toHaveBeenCalledWith('x'.repeat(50));
  });

  it('behält den Teamnamen im Feld, wenn das Erfassen abgelehnt wird (US-6)', async () => {
    const fixture = await render([RAKETE]);
    repository.add.mockRejectedValue(
      new HttpErrorResponse({
        status: 400,
        error: { message: 'Ein Team mit diesem Namen gibt es schon.' },
      }),
    );

    const el = await addTeam(fixture, 'rakete');

    expect(addForm(el).querySelector('input')!.value).toBe('rakete');
  });

  it('lehnt beim Umbenennen einen leeren Namen im Formular ab (US-6)', async () => {
    const fixture = await render([RAKETE]);
    const el = fixture.nativeElement as HTMLElement;
    await click(fixture, button(row(el, 'Rakete'), 'Umbenennen'));
    type(rows(el)[0].querySelector('input')!, '   ');

    await click(fixture, button(rows(el)[0], 'Speichern'));

    expect(repository.rename).not.toHaveBeenCalled();
    expect(rows(el)[0].textContent).toContain('Der Teamname darf nicht leer sein.');
  });

  it('lehnt beim Umbenennen einen Namen mit 51 Zeichen im Formular ab (US-6)', async () => {
    const fixture = await render([RAKETE]);
    const el = fixture.nativeElement as HTMLElement;
    await click(fixture, button(row(el, 'Rakete'), 'Umbenennen'));
    type(rows(el)[0].querySelector('input')!, 'x'.repeat(51));

    await click(fixture, button(rows(el)[0], 'Speichern'));

    expect(repository.rename).not.toHaveBeenCalled();
    expect(rows(el)[0].textContent).toContain('Der Teamname darf höchstens 50 Zeichen lang sein.');
  });

  it('belegt das Feld beim Umbenennen mit dem bisherigen Namen vor (US-6)', async () => {
    const fixture = await render([RAKETE]);
    const el = fixture.nativeElement as HTMLElement;

    await click(fixture, button(row(el, 'Rakete'), 'Umbenennen'));

    expect(rows(el)[0].querySelector('input')!.value).toBe('Rakete');
  });

  it('zeigt die Meldung des Servers als Alarm, wenn das Erfassen abgelehnt wird (US-6)', async () => {
    const fixture = await render([RAKETE]);
    repository.add.mockRejectedValue(
      new HttpErrorResponse({
        status: 400,
        error: { message: 'Ein Team mit diesem Namen gibt es schon.' },
      }),
    );

    const el = await addTeam(fixture, 'rakete');

    expect(el.querySelector('[role="alert"]')?.textContent).toContain(
      'Ein Team mit diesem Namen gibt es schon.',
    );
  });

  it('entfernt ein Team erst nach der Bestätigung (US-6)', async () => {
    const fixture = await render([RAKETE], []);
    repository.remove.mockResolvedValue(undefined);
    const el = fixture.nativeElement as HTMLElement;

    await click(fixture, button(row(el, 'Rakete'), 'Entfernen'));

    expect(repository.remove).not.toHaveBeenCalled();
    expect(row(el, 'Rakete').textContent).toContain('Wirklich entfernen?');

    await click(fixture, button(row(el, 'Rakete'), 'Ja, entfernen'));

    expect(repository.remove).toHaveBeenCalledWith('r');
    expect(rows(el)).toHaveLength(0);
    expect(el.textContent).toContain('Noch keine Teams. Erfasse das erste Team.');
  });

  // Delta US-6 (Issue #13), AK 5: fester Hinweistext, keine Zählung. Der Wortlaut ist nicht festgelegt, darum
  // prüft der Test die drei Aussagen: eingeplante Features, in allen PIs, zurück ins Backlog.
  it('weist in der Bestätigung darauf hin, dass die eingeplanten Features in allen PIs ins Backlog zurückgehen (US-6)', async () => {
    const fixture = await render([RAKETE, ZEBRA]);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).not.toMatch(/eingeplant/i);

    await click(fixture, button(row(el, 'Rakete'), 'Entfernen'));

    const text = row(el, 'Rakete').textContent ?? '';
    expect(text).toContain('Wirklich entfernen?');
    expect(text).toMatch(/eingeplanten Features/);
    expect(text).toMatch(/allen PIs/);
    expect(text).toMatch(/Backlog/);
    expect(row(el, 'Zebra').textContent).not.toMatch(/eingeplant/i);
    expect(repository.remove).not.toHaveBeenCalled();
  });

  it('behält das Team, wenn das Entfernen abgebrochen wird (US-6)', async () => {
    const fixture = await render([RAKETE]);
    const el = fixture.nativeElement as HTMLElement;
    await click(fixture, button(row(el, 'Rakete'), 'Entfernen'));

    await click(fixture, button(row(el, 'Rakete'), 'Abbrechen'));

    expect(repository.remove).not.toHaveBeenCalled();
    expect(row(el, 'Rakete').textContent).not.toContain('Wirklich entfernen?');
    expect(hasButton(row(el, 'Rakete'), 'Entfernen')).toBe(true);
  });

  it('fragt nur beim gewählten Team nach der Bestätigung (US-6)', async () => {
    const fixture = await render([RAKETE, ZEBRA]);
    const el = fixture.nativeElement as HTMLElement;

    await click(fixture, button(row(el, 'Zebra'), 'Entfernen'));

    expect(row(el, 'Zebra').textContent).toContain('Wirklich entfernen?');
    expect(row(el, 'Rakete').textContent).not.toContain('Wirklich entfernen?');
  });

  it('benennt ein Team inline um (US-6)', async () => {
    const fixture = await render([RAKETE], [{ id: 'r', name: 'Komet' }]);
    repository.rename.mockResolvedValue({ id: 'r', name: 'Komet' });
    const el = fixture.nativeElement as HTMLElement;
    await click(fixture, button(row(el, 'Rakete'), 'Umbenennen'));

    type(rows(el)[0].querySelector('input')!, 'Komet');
    await click(fixture, button(rows(el)[0], 'Speichern'));

    expect(repository.rename).toHaveBeenCalledWith('r', 'Komet');
    expect(rows(el).map((r) => r.textContent)).toEqual([expect.stringContaining('Komet')]);
    expect(el.textContent).not.toContain('Rakete');
  });

  it('lässt den Namen beim Abbrechen des Umbenennens unverändert (US-6)', async () => {
    const fixture = await render([RAKETE]);
    const el = fixture.nativeElement as HTMLElement;
    await click(fixture, button(row(el, 'Rakete'), 'Umbenennen'));
    type(rows(el)[0].querySelector('input')!, 'Komet');

    await click(fixture, button(rows(el)[0], 'Abbrechen'));

    expect(repository.rename).not.toHaveBeenCalled();
    expect(rows(el)[0].querySelector('input')).toBeNull();
    expect(rows(el)[0].textContent).toContain('Rakete');
  });

  it('zeigt die Meldung des Servers, wenn das Umbenennen abgelehnt wird (US-6)', async () => {
    const fixture = await render([RAKETE, ZEBRA]);
    repository.rename.mockRejectedValue(
      new HttpErrorResponse({
        status: 400,
        error: { message: 'Ein Team mit diesem Namen gibt es schon.' },
      }),
    );
    const el = fixture.nativeElement as HTMLElement;
    await click(fixture, button(row(el, 'Rakete'), 'Umbenennen'));
    type(rows(el)[0].querySelector('input')!, 'zebra');

    await click(fixture, button(rows(el)[0], 'Speichern'));

    expect(repository.rename).toHaveBeenCalledWith('r', 'zebra');
    expect(el.querySelector('[role="alert"]')?.textContent).toContain(
      'Ein Team mit diesem Namen gibt es schon.',
    );
  });
});
