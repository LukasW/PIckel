package ch.grudligstrasse.pickel.domain.model;

import java.time.LocalDate;

/**
 * Eine Iteration eines PI. Wird aus dem PI berechnet, nicht gespeichert; spätere Stories verweisen über PI und
 * {@code number} darauf.
 */
public record Iteration(
        int number, String name, LocalDate startDate, LocalDate endDate, boolean innovationAndPlanning) {}
