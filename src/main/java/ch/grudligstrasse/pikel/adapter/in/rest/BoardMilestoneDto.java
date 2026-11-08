package ch.grudligstrasse.pikel.adapter.in.rest;

import ch.grudligstrasse.pikel.domain.model.Milestone;
import java.time.LocalDate;
import java.util.UUID;

/** Ein Milestone auf dem Board: der Marker steht über der Iteration mit {@code iterationNumber}. */
public record BoardMilestoneDto(UUID id, String name, LocalDate date, int iterationNumber) {

    static BoardMilestoneDto from(Milestone milestone, int iterationNumber) {
        return new BoardMilestoneDto(milestone.id(), milestone.name(), milestone.date(), iterationNumber);
    }
}
