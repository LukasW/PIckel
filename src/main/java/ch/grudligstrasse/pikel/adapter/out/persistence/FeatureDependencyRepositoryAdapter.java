package ch.grudligstrasse.pikel.adapter.out.persistence;

import ch.grudligstrasse.pikel.domain.model.FeatureDependency;
import ch.grudligstrasse.pikel.domain.port.out.FeatureDependencyRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FeatureDependencyRepositoryAdapter
        implements FeatureDependencyRepository,
                PanacheRepositoryBase<FeatureDependencyEntity, FeatureDependencyEntity.Key> {

    @Override
    public List<FeatureDependency> findByProgramIncrement(UUID programIncrementId) {
        return find(
                        "select d from FeatureDependencyEntity d, FeatureEntity f"
                                + " where f.id = d.needingFeatureId and f.programIncrementId = ?1",
                        programIncrementId)
                .stream()
                .map(entity -> new FeatureDependency(entity.needingFeatureId, entity.neededFeatureId))
                .toList();
    }

    @Override
    public void save(FeatureDependency dependency) {
        FeatureDependencyEntity entity = new FeatureDependencyEntity();
        entity.needingFeatureId = dependency.needingFeatureId();
        entity.neededFeatureId = dependency.neededFeatureId();
        persist(entity);
    }

    @Override
    public void remove(UUID needingFeatureId, UUID neededFeatureId) {
        delete("needingFeatureId = ?1 and neededFeatureId = ?2", needingFeatureId, neededFeatureId);
    }
}
