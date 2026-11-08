package ch.grudligstrasse.pikel.application.service;

import ch.grudligstrasse.pikel.domain.model.Feature;
import ch.grudligstrasse.pikel.domain.model.ProgramBoard;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrement;
import ch.grudligstrasse.pikel.domain.port.in.BoardUseCase;
import ch.grudligstrasse.pikel.domain.port.in.FeatureUseCase;
import ch.grudligstrasse.pikel.domain.port.in.ProgramIncrementUseCase;
import ch.grudligstrasse.pikel.domain.port.in.TeamUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class BoardService implements BoardUseCase {

    private final ProgramIncrementUseCase programIncrements;
    private final TeamUseCase teams;
    private final FeatureUseCase features;

    public BoardService(ProgramIncrementUseCase programIncrements, TeamUseCase teams, FeatureUseCase features) {
        this.programIncrements = programIncrements;
        this.teams = teams;
        this.features = features;
    }

    @Override
    public ProgramBoard get(UUID programIncrementId) {
        ProgramIncrement programIncrement = programIncrements.get(programIncrementId);
        // Reihenfolgen liefern die Ports (Teams alphabetisch, Backlog nach WSJF); hier nicht ein zweites Mal sortieren.
        List<Feature> backlog = features.listBacklog(programIncrementId).stream()
                .filter(feature -> !feature.scheduled())
                .toList();
        return new ProgramBoard(programIncrement, teams.listAll(), programIncrement.iterations(), backlog);
    }
}
