package ch.grudligstrasse.pickel.application.service;

import ch.grudligstrasse.pickel.domain.model.Capacity;
import ch.grudligstrasse.pickel.domain.model.Iteration;
import ch.grudligstrasse.pickel.domain.model.ProgramIncrement;
import ch.grudligstrasse.pickel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pickel.domain.model.Team;
import ch.grudligstrasse.pickel.domain.model.TeamNotFoundException;
import ch.grudligstrasse.pickel.domain.port.in.CapacityUseCase;
import ch.grudligstrasse.pickel.domain.port.in.TeamUseCase;
import ch.grudligstrasse.pickel.domain.port.out.CapacityRepository;
import ch.grudligstrasse.pickel.domain.port.out.ProgramIncrementRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class CapacityService implements CapacityUseCase {

    private final CapacityRepository capacities;
    private final ProgramIncrementRepository programIncrements;
    private final TeamUseCase teams;

    public CapacityService(
            CapacityRepository capacities, ProgramIncrementRepository programIncrements, TeamUseCase teams) {
        this.capacities = capacities;
        this.programIncrements = programIncrements;
        this.teams = teams;
    }

    // Die volle Matrix entsteht hier, damit «ohne Eintrag 0» (ADR 0005) nur an einem Ort gilt.
    @Override
    public List<Capacity> list(UUID programIncrementId) {
        ProgramIncrement programIncrement = programIncrement(programIncrementId);
        Map<CellKey, Integer> stored = capacities.findByProgramIncrement(programIncrementId).stream()
                .collect(Collectors.toMap(
                        capacity -> new CellKey(capacity.teamId(), capacity.iterationNumber()), Capacity::points));
        List<Team> teamList = teams.listAll();
        List<Capacity> cells = new ArrayList<>();
        for (Iteration iteration : programIncrement.iterations()) {
            if (iteration.innovationAndPlanning()) {
                continue;
            }
            for (Team team : teamList) {
                int points = stored.getOrDefault(new CellKey(team.id(), iteration.number()), 0);
                cells.add(new Capacity(team.id(), iteration.number(), points));
            }
        }
        return List.copyOf(cells);
    }

    @Override
    @Transactional
    public Capacity set(UUID programIncrementId, UUID teamId, int iterationNumber, Integer points) {
        ProgramIncrement programIncrement = programIncrement(programIncrementId);
        if (teams.listAll().stream().noneMatch(team -> team.id().equals(teamId))) {
            throw new TeamNotFoundException();
        }
        Capacity capacity = Capacity.plan(programIncrement, teamId, iterationNumber, points);
        capacities.save(programIncrementId, capacity);
        return capacity;
    }

    private ProgramIncrement programIncrement(UUID id) {
        return programIncrements.findOne(id).orElseThrow(ProgramIncrementNotFoundException::new);
    }

    private record CellKey(UUID teamId, int iterationNumber) {}
}
