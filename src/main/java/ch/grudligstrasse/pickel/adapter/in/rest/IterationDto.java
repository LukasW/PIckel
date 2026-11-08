package ch.grudligstrasse.pickel.adapter.in.rest;

import ch.grudligstrasse.pickel.domain.model.Iteration;
import java.time.LocalDate;

/** Antwort-Darstellung einer Iteration. */
public record IterationDto(
        int number, String name, LocalDate startDate, LocalDate endDate, boolean innovationAndPlanning) {

    static IterationDto from(Iteration iteration) {
        return new IterationDto(
                iteration.number(),
                iteration.name(),
                iteration.startDate(),
                iteration.endDate(),
                iteration.innovationAndPlanning());
    }
}
