import { Board } from '../models/board.model';

/** Driven Port: woher das Board eines PI kommt (heute REST). */
export abstract class BoardRepository {
  abstract get(programIncrementId: string): Promise<Board>;
}
