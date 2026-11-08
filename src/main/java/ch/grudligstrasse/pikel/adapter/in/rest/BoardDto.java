package ch.grudligstrasse.pikel.adapter.in.rest;

import ch.grudligstrasse.pikel.domain.model.ProgramBoard;
import java.util.List;

/** Das Board für die Oberfläche: Zeilen, Spalten, Backlog und Milestones in einem Aufruf. */
public record BoardDto(
        List<TeamDto> teams,
        List<IterationDto> iterations,
        List<FeatureDto> backlog,
        List<BoardMilestoneDto> milestones) {

    static BoardDto from(ProgramBoard board) {
        return new BoardDto(
                board.teams().stream().map(TeamDto::from).toList(),
                board.iterations().stream().map(IterationDto::from).toList(),
                board.backlog().stream().map(FeatureDto::from).toList(),
                board.milestones().stream()
                        .map(milestone -> BoardMilestoneDto.from(milestone, board.iterationNumberOf(milestone)))
                        .toList());
    }
}
