package ch.grudligstrasse.pikel.domain.port.in;

import ch.grudligstrasse.pikel.domain.model.Feature;
import java.util.List;
import java.util.UUID;

/** Driving Port: das Feature-Backlog eines PI. */
public interface FeatureUseCase {

    /**
     * Das Backlog des PI, aufsteigend nach WSJF-Rang (ohne Rang am Ende), bei gleichem Rang nach Titel.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es das PI nicht gibt
     */
    List<Feature> listBacklog(UUID programIncrementId);

    /**
     * Erfasst ein Feature im PI; Rang und Schätzung sind optional (null = fehlt).
     *
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es das PI nicht gibt
     * @throws ch.grudligstrasse.pikel.domain.model.InvalidFeatureException bei ungültigen Angaben
     */
    Feature add(
            UUID programIncrementId,
            String title,
            String description,
            Integer wsjfRank,
            String owner,
            Integer estimate);

    /**
     * Ändert ein Feature des PI mit denselben Regeln wie beim Erfassen.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es das PI nicht gibt
     * @throws ch.grudligstrasse.pikel.domain.model.FeatureNotFoundException wenn es das Feature im PI nicht gibt
     * @throws ch.grudligstrasse.pikel.domain.model.InvalidFeatureException bei ungültigen Angaben
     */
    Feature update(
            UUID programIncrementId,
            UUID featureId,
            String title,
            String description,
            Integer wsjfRank,
            String owner,
            Integer estimate);

    /**
     * Löscht ein Feature des PI.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es das PI nicht gibt
     * @throws ch.grudligstrasse.pikel.domain.model.FeatureNotFoundException wenn es das Feature im PI nicht gibt
     */
    void remove(UUID programIncrementId, UUID featureId);

    /**
     * Plant ein Feature des PI in die Zelle Team × Iteration ein; eine bestehende Einplanung wird ersetzt.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es das PI nicht gibt
     * @throws ch.grudligstrasse.pikel.domain.model.FeatureNotFoundException wenn es das Feature im PI nicht gibt
     * @throws ch.grudligstrasse.pikel.domain.model.TeamNotFoundException wenn es das Team nicht gibt
     * @throws ch.grudligstrasse.pikel.domain.model.InvalidFeatureException bei fehlendem Team oder fehlender Nummer, bei
     *     der IP-Iteration oder einer Nummer, die es im PI nicht gibt
     */
    Feature schedule(UUID programIncrementId, UUID featureId, UUID teamId, Integer iterationNumber);

    /**
     * Nimmt ein Feature des PI zurück ins Backlog; ein Feature ohne Einplanung bleibt unverändert.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es das PI nicht gibt
     * @throws ch.grudligstrasse.pikel.domain.model.FeatureNotFoundException wenn es das Feature im PI nicht gibt
     */
    Feature unschedule(UUID programIncrementId, UUID featureId);

    /**
     * Importiert die Features einer CSV-Datei ans Ende des Backlogs (kein Abgleich mit bestehenden); alles oder nichts.
     *
     * @return die importierten Features in Dateireihenfolge
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es das PI nicht gibt
     * @throws ch.grudligstrasse.pikel.domain.model.InvalidFeatureException wenn die Datei oder eine Zeile ungültig ist
     */
    List<Feature> importCsv(UUID programIncrementId, String csv);
}
