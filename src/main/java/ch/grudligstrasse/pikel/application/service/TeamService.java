package ch.grudligstrasse.pikel.application.service;

import ch.grudligstrasse.pikel.domain.model.DuplicateTeamNameException;
import ch.grudligstrasse.pikel.domain.model.Team;
import ch.grudligstrasse.pikel.domain.model.TeamNotFoundException;
import ch.grudligstrasse.pikel.domain.port.in.TeamUseCase;
import ch.grudligstrasse.pikel.domain.port.out.TeamRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class TeamService implements TeamUseCase {

    /** Alphabetisch wie im Deutschen: ohne Gross-/Kleinschreibung, Umlaute beim Grundbuchstaben. */
    private static final Locale SORT_LOCALE = Locale.forLanguageTag("de-CH");

    private final TeamRepository repository;

    public TeamService(TeamRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Team> listAll() {
        Collator collator = Collator.getInstance(SORT_LOCALE);
        collator.setStrength(Collator.SECONDARY);
        return repository.findAllTeams().stream()
                .sorted(Comparator.comparing(Team::name, collator))
                .toList();
    }

    @Override
    @Transactional
    public Team add(String name) {
        Team team = Team.create(name);
        ensureNameIsFree(team);
        repository.save(team);
        return team;
    }

    @Override
    @Transactional
    public Team rename(UUID id, String name) {
        Team renamed =
                repository.findOne(id).orElseThrow(TeamNotFoundException::new).rename(name);
        ensureNameIsFree(renamed);
        repository.save(renamed);
        return renamed;
    }

    @Override
    @Transactional
    public void remove(UUID id) {
        if (!repository.remove(id)) {
            throw new TeamNotFoundException();
        }
    }

    // Ein Treffer mit derselben ID ist das Team selbst: Umbenennen in anderer Schreibweise bleibt erlaubt.
    private void ensureNameIsFree(Team team) {
        repository.findByNameIgnoreCase(team.name()).ifPresent(existing -> {
            if (!existing.id().equals(team.id())) {
                throw new DuplicateTeamNameException();
            }
        });
    }
}
