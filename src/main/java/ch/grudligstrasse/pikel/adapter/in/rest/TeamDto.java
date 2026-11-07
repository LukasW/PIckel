package ch.grudligstrasse.pikel.adapter.in.rest;

import ch.grudligstrasse.pikel.domain.model.Team;
import java.util.UUID;

public record TeamDto(UUID id, String name) {

    static TeamDto from(Team team) {
        return new TeamDto(team.id(), team.name());
    }
}
