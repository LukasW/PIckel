package ch.grudligstrasse.pikel.adapter.out.persistence;

import ch.grudligstrasse.pikel.domain.model.ProgramIncrement;
import ch.grudligstrasse.pikel.domain.port.out.ProgramIncrementRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProgramIncrementRepositoryAdapter
        implements ProgramIncrementRepository, PanacheRepositoryBase<ProgramIncrementEntity, UUID> {

    @Override
    public List<ProgramIncrement> findAllOrderedByStartDate() {
        return listAll(Sort.ascending("startDate")).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ProgramIncrement> findOne(UUID id) {
        return findByIdOptional(id).map(this::toDomain);
    }

    @Override
    public void save(ProgramIncrement programIncrement) {
        persist(toEntity(programIncrement));
    }

    private ProgramIncrement toDomain(ProgramIncrementEntity entity) {
        return new ProgramIncrement(
                entity.id, entity.name, entity.startDate, entity.endDate, entity.iterationLengthWeeks);
    }

    private ProgramIncrementEntity toEntity(ProgramIncrement programIncrement) {
        ProgramIncrementEntity entity = new ProgramIncrementEntity();
        entity.id = programIncrement.id();
        entity.name = programIncrement.name();
        entity.startDate = programIncrement.startDate();
        entity.endDate = programIncrement.endDate();
        entity.iterationLengthWeeks = programIncrement.iterationLengthWeeks();
        return entity;
    }
}
