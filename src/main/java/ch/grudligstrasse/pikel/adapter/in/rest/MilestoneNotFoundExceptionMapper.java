package ch.grudligstrasse.pikel.adapter.in.rest;

import ch.grudligstrasse.pikel.domain.model.MilestoneNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Unbekannter Milestone → 404 mit Meldung für die Oberfläche. */
@Provider
public class MilestoneNotFoundExceptionMapper implements ExceptionMapper<MilestoneNotFoundException> {

    @Override
    public Response toResponse(MilestoneNotFoundException exception) {
        return ErrorResponse.of(Response.Status.NOT_FOUND, exception.getMessage());
    }
}
