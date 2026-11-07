package ch.grudligstrasse.pikel.adapter.in.rest;

import java.time.LocalDate;

/**
 * Anfrage zum Anlegen eines PI. Ohne Bean-Validation-Annotationen: Die Regeln stehen nur in der Domain, sonst gäbe es
 * zwei Orte mit verschiedenen Fehlermeldungen.
 */
public record PlanProgramIncrementRequest(String name, LocalDate startDate, LocalDate endDate) {}
