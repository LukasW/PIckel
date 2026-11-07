package ch.grudligstrasse.pikel.adapter.out.persistence;

import ch.grudligstrasse.pikel.domain.model.Capacity;
import ch.grudligstrasse.pikel.domain.port.out.CapacityRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CapacityRepositoryAdapter
        implements CapacityRepository, PanacheRepositoryBase<CapacityEntity, CapacityEntity.Key> {

    @Override
    public List<Capacity> findByProgramIncrement(UUID programIncrementId) {
        return list("programIncrementId", programIncrementId).stream()
                .map(entity -> new Capacity(entity.teamId, entity.iterationNumber, entity.points))
                .toList();
    }

    @Override
    public void save(UUID programIncrementId, Capacity capacity) {
        CapacityEntity entity = find(
                        "programIncrementId = ?1 and teamId = ?2 and iterationNumber = ?3",
                        programIncrementId,
                        capacity.teamId(),
                        capacity.iterationNumber())
                .firstResultOptional()
                .orElseGet(CapacityEntity::new);
        entity.programIncrementId = programIncrementId;
        entity.teamId = capacity.teamId();
        entity.iterationNumber = capacity.iterationNumber();
        entity.points = capacity.points();
        persist(entity);
    }
}
