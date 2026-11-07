package ch.grudligstrasse.pikel.adapter.in.rest;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Einheitlicher Fehlerkörper der REST-API: jede 400 und 404 trägt {@code { "message": … }} für die Oberfläche. */
public record ErrorResponse(String message) {

    static Response badRequest(String message) {
        return of(Response.Status.BAD_REQUEST, message);
    }

    static Response of(Response.Status status, String message) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorResponse(message))
                .build();
    }
}
