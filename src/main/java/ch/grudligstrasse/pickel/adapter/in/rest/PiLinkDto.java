package ch.grudligstrasse.pickel.adapter.in.rest;

import ch.grudligstrasse.pickel.domain.model.PiLink;
import java.util.UUID;

/** Antwort-Darstellung eines Links am PI. */
public record PiLinkDto(UUID id, String title, String url) {

    static PiLinkDto from(PiLink link) {
        return new PiLinkDto(link.id(), link.title(), link.url());
    }
}
