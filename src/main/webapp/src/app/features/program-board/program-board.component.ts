import { CdkDrag, CdkDragDrop, CdkDropList, CdkDropListGroup } from '@angular/cdk/drag-drop';
import { DatePipe } from '@angular/common';
import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { Board } from '../../core/models/board.model';
import { Feature } from '../../core/models/feature.model';
import { BoardMilestone } from '../../core/models/milestone.model';
import { BoardService } from '../../core/services/board.service';
import { BoardCardComponent, SchedulingChoice } from './board-card.component';
import { emphasisOf, Emphasis, lineBetween, Rect } from './dependency-lines';

/** Ziel eines Ablegens: das Backlog oder eine Zelle Team × Iteration. */
type DropTarget = 'backlog' | SchedulingChoice;

/** Eine gezeichnete Abhängigkeit: SVG-Pfad von der liefernden zur brauchenden Karte (US-17). */
interface DrawnLine {
  readonly from: string;
  readonly to: string;
  readonly path: string;
  readonly emphasis: Emphasis;
  /** Die Lieferung kommt später als der Bedarf (US-18): Die Linie ist rot und gestrichelt. */
  readonly conflict: boolean;
}

/**
 * Das Program Board eines PI (US-12): Teams × Iterationen, links das nicht eingeplante Backlog, Milestones als
 * Marker im Kopf ihrer Iteration (US-15). Karten lassen sich in Zellen ziehen und einplanen (US-13).
 * Abhängigkeiten zwischen eingeplanten Karten stehen als Linien über dem Raster (US-17).
 */
@Component({
  selector: 'app-program-board',
  imports: [BoardCardComponent, CdkDrag, CdkDropList, CdkDropListGroup, DatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './program-board.component.html',
})
export class ProgramBoardComponent {
  protected readonly service = inject(BoardService);

  readonly id = input.required<string>();

  protected readonly backlogTarget: DropTarget = 'backlog';

  /** Karte unter der Maus oder mit dem Tastaturfokus; ihre Linien werden hervorgehoben (US-17). */
  protected readonly focusedFeatureId = signal<string | undefined>(undefined);

  private readonly grid = viewChild<ElementRef<HTMLElement>>('grid');
  /** Gemessene Karten im Raster, relativ zum Raster, damit waagrechtes Scrollen nichts verschiebt. */
  private readonly cardRects = signal<ReadonlyMap<string, Rect>>(new Map(), { equal: sameRects });

  protected readonly lines = computed<readonly DrawnLine[]>(() => {
    const rects = this.cardRects();
    const focused = this.focusedFeatureId();
    return this.service.dependencyLinks().flatMap((link) => {
      const line = lineBetween(rects.get(link.neededFeatureId), rects.get(link.needingFeatureId));
      return line
        ? [
            {
              from: link.neededFeatureId,
              to: link.needingFeatureId,
              path: `M ${line.from.x} ${line.from.y} L ${line.to.x} ${line.to.y}`,
              emphasis: emphasisOf(link, focused),
              conflict: link.conflict,
            },
          ]
        : [];
    });
  });

  constructor() {
    effect(() => void this.service.load(this.id()));
    // Nach jeder Änderung des Boards (auch optimistisch beim Einplanen) die Karten neu messen.
    afterRenderEffect(() => {
      this.service.board();
      this.measure();
    });
    this.observeResize();
  }

  protected focus(featureId: string): void {
    this.focusedFeatureId.set(featureId);
  }

  protected blur(event?: FocusEvent): void {
    // Wandert der Fokus innerhalb derselben Karte weiter (etwa ins Formular), bleibt sie hervorgehoben.
    const item = event?.currentTarget as Node | null | undefined;
    const next = event?.relatedTarget as Node | null | undefined;
    if (item && next && item.contains(next)) {
      return;
    }
    this.focusedFeatureId.set(undefined);
  }

  private measure(): void {
    const grid = this.grid()?.nativeElement;
    if (!grid) {
      this.cardRects.set(new Map());
      return;
    }
    const origin = grid.getBoundingClientRect();
    const rects = new Map<string, Rect>();
    for (const card of Array.from(grid.querySelectorAll<HTMLElement>('[data-feature-id]'))) {
      const r = card.getBoundingClientRect();
      rects.set(card.dataset['featureId']!, {
        x: r.left - origin.left,
        y: r.top - origin.top,
        width: r.width,
        height: r.height,
      });
    }
    this.cardRects.set(rects);
  }

  /** Ändert sich die Grösse des Rasters (Fenster, Inhalt), liegen die Karten anders: neu messen. */
  private observeResize(): void {
    // jsdom (Vitest) kennt keinen ResizeObserver; dort genügt die Messung nach dem Rendern.
    if (typeof ResizeObserver === 'undefined') {
      return;
    }
    const observer = new ResizeObserver(() => this.measure());
    effect(() => {
      observer.disconnect();
      const grid = this.grid()?.nativeElement;
      if (grid) {
        observer.observe(grid);
      }
    });
    inject(DestroyRef).onDestroy(() => observer.disconnect());
  }

  /** Die Milestones über einer Iteration, in der Reihenfolge des Servers (nach Datum). */
  protected milestonesIn(board: Board, iterationNumber: number): readonly BoardMilestone[] {
    return board.milestones.filter((m) => m.iterationNumber === iterationNumber);
  }

  /** Die Karten einer Zelle, in der Reihenfolge des Servers (Backlog-Reihenfolge). */
  protected cardsIn(board: Board, teamId: string, iterationNumber: number): readonly Feature[] {
    return board.scheduled.filter(
      (f) => f.teamId === teamId && f.iterationNumber === iterationNumber,
    );
  }

  protected cell(teamId: string, iterationNumber: number): DropTarget {
    return { teamId, iterationNumber };
  }

  protected drop(event: CdkDragDrop<DropTarget>): void {
    // Ausserhalb einer Ablage losgelassen: Das CDK meldet sonst die zuletzt überfahrene Liste als Ziel, und die Karte
    // landete etwa neben der IP-Iteration. Sie bleibt am Ort; über der IP-Zelle mit der Meldung aus AK 3.
    if (!event.isPointerOverContainer) {
      if (isInnovationAndPlanningAt(event.dropPoint)) {
        this.service.rejectInnovationAndPlanning();
      }
      return;
    }
    if (event.previousContainer === event.container) {
      return;
    }
    const feature = event.item.data as Feature;
    const target = event.container.data;
    if (target === 'backlog') {
      void this.service.unschedule(feature);
    } else {
      void this.service.schedule(feature, target.teamId, target.iterationNumber);
    }
  }
}

/** Liegt der Punkt in einer Zelle der IP-Iteration? Die IP-Zellen sind keine Ablage und tragen deshalb ein Attribut. */
function isInnovationAndPlanningAt(point: { x: number; y: number }): boolean {
  return !!document.elementFromPoint(point.x, point.y)?.closest('[data-innovation-and-planning]');
}

/** Gleiche Messung → kein neues Signal, sonst rendert die Messung nach dem Rendern endlos. */
function sameRects(a: ReadonlyMap<string, Rect>, b: ReadonlyMap<string, Rect>): boolean {
  if (a.size !== b.size) {
    return false;
  }
  for (const [id, r] of a) {
    const other = b.get(id);
    if (
      !other ||
      other.x !== r.x ||
      other.y !== r.y ||
      other.width !== r.width ||
      other.height !== r.height
    ) {
      return false;
    }
  }
  return true;
}
