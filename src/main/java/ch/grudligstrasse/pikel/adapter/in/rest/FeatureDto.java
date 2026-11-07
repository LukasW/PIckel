package ch.grudligstrasse.pikel.adapter.in.rest;

import ch.grudligstrasse.pikel.domain.model.Feature;
import java.util.UUID;

/** Ein Feature für die Oberfläche; fehlender Rang und fehlende Schätzung sind {@code null}. */
public record FeatureDto(
        UUID id,
        UUID programIncrementId,
        String title,
        String description,
        String owner,
        Integer wsjfRank,
        Integer estimate,
        boolean scheduled) {

    public static FeatureDto from(Feature feature) {
        return new FeatureDto(
                feature.id(),
                feature.programIncrementId(),
                feature.title(),
                feature.description(),
                feature.owner(),
                feature.wsjfRank().isPresent() ? feature.wsjfRank().getAsInt() : null,
                feature.estimate().isPresent() ? feature.estimate().getAsInt() : null,
                feature.scheduled());
    }
}
