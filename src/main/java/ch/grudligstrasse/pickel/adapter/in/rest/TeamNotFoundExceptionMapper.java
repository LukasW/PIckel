package ch.grudligstrasse.pickel.adapter.in.rest;

import ch.grudligstrasse.pickel.domain.model.TeamNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Unbekanntes Team → 404 mit Meldung für die Oberfläche. */
@Provider
public class TeamNotFoundExceptionMapper implements ExceptionMapper<TeamNotFoundException> {

    @Override
    public Response toResponse(TeamNotFoundException exception) {
        return ErrorResponse.of(Response.Status.NOT_FOUND, exception.getMessage());
    }
}
