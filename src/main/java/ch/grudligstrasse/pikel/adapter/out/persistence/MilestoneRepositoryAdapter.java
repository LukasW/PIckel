package ch.grudligstrasse.pikel.adapter.out.persistence;

import ch.grudligstrasse.pikel.domain.model.Milestone;
import ch.grudligstrasse.pikel.domain.port.out.MilestoneRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class MilestoneRepositoryAdapter implements MilestoneRepository, PanacheRepositoryBase<MilestoneEntity, UUID> {

    @Override
    public List<Milestone> findByProgramIncrement(UUID programIncrementId) {
        return list("programIncrementId", Sort.ascending("date", "name"), programIncrementId).stream()
                .map(MilestoneRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<Milestone> findOne(UUID programIncrementId, UUID milestoneId) {
        return find("id = ?1 and programIncrementId = ?2", milestoneId, programIncrementId)
                .firstResultOptional()
                .map(MilestoneRepositoryAdapter::toDomain);
    }

    @Override
    public void save(Milestone milestone) {
        Optional<MilestoneEntity> existing = findByIdOptional(milestone.id());
        MilestoneEntity entity = existing.orElseGet(MilestoneEntity::new);
        entity.id = milestone.id();
        entity.programIncrementId = milestone.programIncrementId();
        entity.name = milestone.name();
        entity.date = milestone.date();
        if (existing.isEmpty()) {
            persist(entity);
        }
    }

    @Override
    public void delete(UUID milestoneId) {
        deleteById(milestoneId);
    }

    private static Milestone toDomain(MilestoneEntity entity) {
        return new Milestone(entity.id, entity.programIncrementId, entity.name, entity.date);
    }
}
