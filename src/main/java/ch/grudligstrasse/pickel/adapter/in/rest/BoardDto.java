package ch.grudligstrasse.pickel.adapter.in.rest;

import ch.grudligstrasse.pickel.domain.model.ProgramBoard;
import java.util.List;

/** Das Board für die Oberfläche: Zeilen, Spalten, Backlog, Eingeplante und Milestones in einem Aufruf. */
public record BoardDto(
        List<TeamDto> teams,
        List<IterationDto> iterations,
        List<FeatureDto> backlog,
        List<FeatureDto> scheduled,
        List<BoardMilestoneDto> milestones,
        List<CapacityDto> capacities,
        List<FeatureDependencyDto> dependencies) {

    static BoardDto from(ProgramBoard board) {
        return new BoardDto(
                board.teams().stream().map(TeamDto::from).toList(),
                board.iterations().stream().map(IterationDto::from).toList(),
                board.backlog().stream().map(FeatureDto::from).toList(),
                board.scheduled().stream().map(FeatureDto::from).toList(),
                board.milestones().stream()
                        .map(milestone -> BoardMilestoneDto.from(milestone, board.iterationNumberOf(milestone)))
                        .toList(),
                board.capacities().stream().map(CapacityDto::from).toList(),
                board.dependencies().stream().map(FeatureDependencyDto::from).toList());
    }
}
