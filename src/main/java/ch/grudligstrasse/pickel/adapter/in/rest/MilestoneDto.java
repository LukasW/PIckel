package ch.grudligstrasse.pickel.adapter.in.rest;

import ch.grudligstrasse.pickel.domain.model.Milestone;
import java.time.LocalDate;
import java.util.UUID;

/** Antwort-Darstellung eines Milestones. */
public record MilestoneDto(UUID id, String name, LocalDate date) {

    static MilestoneDto from(Milestone milestone) {
        return new MilestoneDto(milestone.id(), milestone.name(), milestone.date());
    }
}
