package ch.grudligstrasse.pikel.adapter.in.rest;

import ch.grudligstrasse.pikel.domain.model.InvalidMilestoneException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Fachliche Regelverletzung bei einem Milestone → 400 mit Meldung für die Oberfläche. */
@Provider
public class InvalidMilestoneExceptionMapper implements ExceptionMapper<InvalidMilestoneException> {

    @Override
    public Response toResponse(InvalidMilestoneException exception) {
        return ErrorResponse.badRequest(exception.getMessage());
    }
}
