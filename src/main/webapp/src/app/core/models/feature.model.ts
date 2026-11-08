/** Ein Feature im Backlog eines PI; `wsjfRank` und `estimate` (Story Points) sind optional, Texte leer, wenn fehlend. */
export interface Feature {
  id: string;
  programIncrementId: string;
  title: string;
  description: string;
  owner: string;
  wsjfRank?: number;
  estimate?: number;
  scheduled: boolean;
  /** Die Zelle auf dem Program Board; beides fehlt im Backlog. */
  teamId?: string;
  iterationNumber?: number;
}

/** Angaben zum Erfassen; fehlende optionale Angaben fehlen im Objekt. */
export interface NewFeature {
  title: string;
  description?: string;
  owner?: string;
  wsjfRank?: number;
  estimate?: number;
}
