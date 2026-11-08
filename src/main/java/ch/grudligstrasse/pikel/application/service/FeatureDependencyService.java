package ch.grudligstrasse.pikel.application.service;

import ch.grudligstrasse.pikel.domain.model.Feature;
import ch.grudligstrasse.pikel.domain.model.FeatureDependency;
import ch.grudligstrasse.pikel.domain.model.FeatureNotFoundException;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pikel.domain.port.in.FeatureDependencyUseCase;
import ch.grudligstrasse.pikel.domain.port.out.FeatureDependencyRepository;
import ch.grudligstrasse.pikel.domain.port.out.FeatureRepository;
import ch.grudligstrasse.pikel.domain.port.out.ProgramIncrementRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FeatureDependencyService implements FeatureDependencyUseCase {

    private final FeatureDependencyRepository dependencies;
    private final FeatureRepository features;
    private final ProgramIncrementRepository programIncrements;

    public FeatureDependencyService(
            FeatureDependencyRepository dependencies,
            FeatureRepository features,
            ProgramIncrementRepository programIncrements) {
        this.dependencies = dependencies;
        this.features = features;
        this.programIncrements = programIncrements;
    }

    @Override
    public List<FeatureDependency> list(UUID programIncrementId) {
        ensureProgramIncrementExists(programIncrementId);
        return dependencies.findByProgramIncrement(programIncrementId);
    }

    @Override
    @Transactional
    public FeatureDependency add(UUID programIncrementId, UUID needingFeatureId, UUID neededFeatureId) {
        ensureProgramIncrementExists(programIncrementId);
        // Das brauchende Feature ist über die Adresse des PI angesprochen: aus einem anderen PI ist es unbekannt.
        // Das gebrauchte Feature in einem anderen PI ist dagegen eine fachliche Regel (Meldung aus der Domain).
        Feature needing = features.findOne(needingFeatureId)
                .filter(feature -> feature.programIncrementId().equals(programIncrementId))
                .orElseThrow(FeatureNotFoundException::new);
        Feature needed = features.findOne(neededFeatureId).orElseThrow(FeatureNotFoundException::new);
        FeatureDependency dependency =
                FeatureDependency.between(needing, needed, dependencies.findByProgramIncrement(programIncrementId));
        dependencies.save(dependency);
        return dependency;
    }

    @Override
    @Transactional
    public void remove(UUID programIncrementId, UUID needingFeatureId, UUID neededFeatureId) {
        ensureProgramIncrementExists(programIncrementId);
        // Wie beim Erfassen: über die Adresse eines anderen PI ist das brauchende Feature unbekannt.
        // Ein ganz unbekanntes Feature hat keine Abhängigkeiten mehr (Kaskade); Entfernen bleibt dann idempotent.
        boolean ofOtherProgramIncrement = features.findOne(needingFeatureId)
                .filter(feature -> !feature.programIncrementId().equals(programIncrementId))
                .isPresent();
        if (ofOtherProgramIncrement) {
            throw new FeatureNotFoundException();
        }
        dependencies.remove(needingFeatureId, neededFeatureId);
    }

    private void ensureProgramIncrementExists(UUID programIncrementId) {
        if (programIncrements.findOne(programIncrementId).isEmpty()) {
            throw new ProgramIncrementNotFoundException();
        }
    }
}
