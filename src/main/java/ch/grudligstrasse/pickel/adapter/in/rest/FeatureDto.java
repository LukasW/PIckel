package ch.grudligstrasse.pickel.adapter.in.rest;

import ch.grudligstrasse.pickel.domain.model.Feature;
import ch.grudligstrasse.pickel.domain.model.Scheduling;
import java.util.UUID;

/** Ein Feature für die Oberfläche; fehlender Rang und fehlende Schätzung sowie die Zelle im Backlog
 * (Team, Iterationsnummer) sind {@code null}. */
public record FeatureDto(
        UUID id,
        UUID programIncrementId,
        String title,
        String description,
        String owner,
        Integer wsjfRank,
        Integer estimate,
        boolean scheduled,
        UUID teamId,
        Integer iterationNumber) {

    public static FeatureDto from(Feature feature) {
        return new FeatureDto(
                feature.id(),
                feature.programIncrementId(),
                feature.title(),
                feature.description(),
                feature.owner(),
                feature.wsjfRank().isPresent() ? feature.wsjfRank().getAsInt() : null,
                feature.estimate().isPresent() ? feature.estimate().getAsInt() : null,
                feature.scheduled(),
                feature.scheduling().map(Scheduling::teamId).orElse(null),
                feature.scheduling().map(Scheduling::iterationNumber).orElse(null));
    }
}
