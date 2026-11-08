package ch.grudligstrasse.pickel.adapter.in.rest;

import ch.grudligstrasse.pickel.domain.model.InvalidTeamException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Fachliche Regelverletzung bei Teams (auch doppelter Name) → 400 mit Meldung für die Oberfläche. */
@Provider
public class InvalidTeamExceptionMapper implements ExceptionMapper<InvalidTeamException> {

    @Override
    public Response toResponse(InvalidTeamException exception) {
        return ErrorResponse.badRequest(exception.getMessage());
    }
}
