package ch.grudligstrasse.pikel.adapter.in.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Anfrage zum Anlegen eines PI. */
public record PlanProgramIncrementRequest(
        @NotBlank String name, @NotNull LocalDate startDate, @NotNull LocalDate endDate) {}
