package ch.grudligstrasse.pikel.adapter.out.persistence;

import ch.grudligstrasse.pikel.domain.model.Team;
import ch.grudligstrasse.pikel.domain.port.out.TeamRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TeamRepositoryAdapter implements TeamRepository, PanacheRepositoryBase<TeamEntity, UUID> {

    @Override
    public List<Team> findAllTeams() {
        return listAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Team> findOne(UUID id) {
        return findByIdOptional(id).map(this::toDomain);
    }

    @Override
    public Optional<Team> findByNameIgnoreCase(String name) {
        return find("lower(name) = lower(?1)", name).firstResultOptional().map(this::toDomain);
    }

    @Override
    public void save(Team team) {
        TeamEntity entity = findByIdOptional(team.id()).orElseGet(TeamEntity::new);
        entity.id = team.id();
        entity.name = team.name();
        persist(entity);
    }

    @Override
    public boolean remove(UUID id) {
        return deleteById(id);
    }

    private Team toDomain(TeamEntity entity) {
        return new Team(entity.id, entity.name);
    }
}
