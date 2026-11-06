import { TestBed } from '@angular/core/testing';
import { ProgramIncrementsRepository } from '../../core/ports/program-increments.repository';
import { ProgramIncrementsComponent } from './program-increments.component';

describe('ProgramIncrementsComponent', () => {
  async function render(list: unknown[]) {
    TestBed.configureTestingModule({
      imports: [ProgramIncrementsComponent],
      providers: [
        {
          provide: ProgramIncrementsRepository,
          useValue: { list: vi.fn().mockResolvedValue(list), plan: vi.fn() },
        },
      ],
    });
    const fixture = TestBed.createComponent(ProgramIncrementsComponent);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('zeigt einen Leerzustand ohne PIs (US-1)', async () => {
    const el = await render([]);

    expect(el.textContent).toContain('Noch keine PIs');
  });

  it('listet vorhandene PIs (US-1)', async () => {
    const el = await render([
      { id: '1', name: 'PI 27.1', startDate: '2027-01-04', endDate: '2027-03-26' },
    ]);

    const rows = el.querySelectorAll('[data-testid="pi-row"]');
    expect(rows).toHaveLength(1);
    expect(rows[0].textContent).toContain('PI 27.1');
  });
});
