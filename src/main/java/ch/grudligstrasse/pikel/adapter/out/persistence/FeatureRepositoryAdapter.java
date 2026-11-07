package ch.grudligstrasse.pikel.adapter.out.persistence;

import ch.grudligstrasse.pikel.domain.model.Feature;
import ch.grudligstrasse.pikel.domain.port.out.FeatureRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

@ApplicationScoped
public class FeatureRepositoryAdapter implements FeatureRepository, PanacheRepositoryBase<FeatureEntity, UUID> {

    @Override
    public List<Feature> findByProgramIncrement(UUID programIncrementId) {
        return list("programIncrementId", programIncrementId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void save(Feature feature) {
        FeatureEntity entity = findByIdOptional(feature.id()).orElseGet(FeatureEntity::new);
        entity.id = feature.id();
        entity.programIncrementId = feature.programIncrementId();
        entity.title = feature.title();
        entity.description = feature.description();
        entity.owner = feature.owner();
        entity.wsjfRank = feature.wsjfRank().isPresent() ? feature.wsjfRank().getAsInt() : null;
        entity.estimate = feature.estimate().isPresent() ? feature.estimate().getAsInt() : null;
        persist(entity);
    }

    private Feature toDomain(FeatureEntity entity) {
        return new Feature(
                entity.id,
                entity.programIncrementId,
                entity.title,
                entity.description,
                entity.owner,
                toOptional(entity.wsjfRank),
                toOptional(entity.estimate));
    }

    private static OptionalInt toOptional(Integer value) {
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }
}
