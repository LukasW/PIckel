package ch.grudligstrasse.pikel.adapter.in.rest;

import ch.grudligstrasse.pikel.domain.model.ProgramIncrement;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Antwort-Darstellung eines PI. */
public record ProgramIncrementDto(
        UUID id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        int iterationLengthWeeks,
        List<IterationDto> iterations) {

    static ProgramIncrementDto from(ProgramIncrement programIncrement) {
        return new ProgramIncrementDto(
                programIncrement.id(),
                programIncrement.name(),
                programIncrement.startDate(),
                programIncrement.endDate(),
                programIncrement.iterationLengthWeeks(),
                programIncrement.iterations().stream().map(IterationDto::from).toList());
    }
}
