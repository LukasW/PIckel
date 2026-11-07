package ch.grudligstrasse.pikel.application.service;

import ch.grudligstrasse.pikel.domain.model.Feature;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pikel.domain.port.in.FeatureUseCase;
import ch.grudligstrasse.pikel.domain.port.out.FeatureRepository;
import ch.grudligstrasse.pikel.domain.port.out.ProgramIncrementRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FeatureService implements FeatureUseCase {

    /** Nach WSJF-Rang (ohne Rang am Ende), bei Gleichstand nach Titel ohne Gross-/Kleinschreibung. */
    private static final Comparator<Feature> BACKLOG_ORDER = Comparator.comparingInt(
                    (Feature feature) -> feature.wsjfRank().orElse(Integer.MAX_VALUE))
            .thenComparing(Feature::title, String.CASE_INSENSITIVE_ORDER);

    private final FeatureRepository features;
    private final ProgramIncrementRepository programIncrements;

    public FeatureService(FeatureRepository features, ProgramIncrementRepository programIncrements) {
        this.features = features;
        this.programIncrements = programIncrements;
    }

    @Override
    public List<Feature> listBacklog(UUID programIncrementId) {
        ensureProgramIncrementExists(programIncrementId);
        return features.findByProgramIncrement(programIncrementId).stream()
                .sorted(BACKLOG_ORDER)
                .toList();
    }

    @Override
    @Transactional
    public Feature add(
            UUID programIncrementId,
            String title,
            String description,
            Integer wsjfRank,
            String owner,
            Integer estimate) {
        ensureProgramIncrementExists(programIncrementId);
        Feature feature = Feature.create(programIncrementId, title, description, wsjfRank, owner, estimate);
        features.save(feature);
        return feature;
    }

    private void ensureProgramIncrementExists(UUID programIncrementId) {
        if (programIncrements.findOne(programIncrementId).isEmpty()) {
            throw new ProgramIncrementNotFoundException();
        }
    }
}
