package ch.grudligstrasse.pickel.adapter.in.rest;

import ch.grudligstrasse.pickel.domain.model.Capacity;
import java.util.UUID;

public record CapacityDto(UUID teamId, int iterationNumber, int points) {

    static CapacityDto from(Capacity capacity) {
        return new CapacityDto(capacity.teamId(), capacity.iterationNumber(), capacity.points());
    }
}
