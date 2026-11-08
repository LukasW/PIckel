package ch.grudligstrasse.pickel.adapter.out.persistence;

import ch.grudligstrasse.pickel.domain.model.PiLink;
import ch.grudligstrasse.pickel.domain.model.ProgramIncrement;
import ch.grudligstrasse.pickel.domain.port.out.ProgramIncrementRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /** Legt ein neues PI an oder übernimmt den Stand eines bestehenden, samt Links. */
    @Override
    public void save(ProgramIncrement programIncrement) {
        Optional<ProgramIncrementEntity> existing = findByIdOptional(programIncrement.id());
        ProgramIncrementEntity entity = existing.orElseGet(ProgramIncrementEntity::new);
        copy(programIncrement, entity);
        if (existing.isEmpty()) {
            persist(entity);
        }
    }

    private ProgramIncrement toDomain(ProgramIncrementEntity entity) {
        return new ProgramIncrement(
                entity.id,
                entity.name,
                entity.startDate,
                entity.endDate,
                entity.iterationLengthWeeks,
                entity.businessContext,
                entity.artObjectives,
                entity.links.stream()
                        .map(link -> new PiLink(link.id, link.title, link.url))
                        .toList());
    }

    private static void copy(ProgramIncrement programIncrement, ProgramIncrementEntity entity) {
        entity.id = programIncrement.id();
        entity.name = programIncrement.name();
        entity.startDate = programIncrement.startDate();
        entity.endDate = programIncrement.endDate();
        entity.iterationLengthWeeks = programIncrement.iterationLengthWeeks();
        entity.businessContext = programIncrement.businessContext();
        entity.artObjectives = programIncrement.artObjectives();
        // Bestehende Link-Entities wiederverwenden: Hibernate fügt vor dem Löschen ein, ein neues Objekt mit derselben
        // ID verletzte sonst den Primärschlüssel. Die Liste selbst bleibt, damit orphanRemoval greift.
        Map<UUID, PiLinkEntity> known =
                entity.links.stream().collect(Collectors.toMap(link -> link.id, Function.identity()));
        entity.links.clear();
        for (PiLink link : programIncrement.links()) {
            PiLinkEntity linkEntity = known.getOrDefault(link.id(), new PiLinkEntity());
            linkEntity.id = link.id();
            linkEntity.title = link.title();
            linkEntity.url = link.url();
            entity.links.add(linkEntity);
        }
    }
}
