package ch.grudligstrasse.pikel.adapter.in.rest;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Einheitlicher Fehlerkörper der REST-API: jede 400 trägt {@code { "message": … }} für die Oberfläche. */
public record ErrorResponse(String message) {

    static Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorResponse(message))
                .build();
    }
}
