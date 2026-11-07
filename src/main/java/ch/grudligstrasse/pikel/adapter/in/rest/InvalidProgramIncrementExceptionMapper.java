package ch.grudligstrasse.pikel.adapter.in.rest;

import ch.grudligstrasse.pikel.domain.model.InvalidProgramIncrementException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Fachliche Regelverletzung → 400 mit Meldung für die Oberfläche. */
@Provider
public class InvalidProgramIncrementExceptionMapper implements ExceptionMapper<InvalidProgramIncrementException> {

    @Override
    public Response toResponse(InvalidProgramIncrementException exception) {
        return ErrorResponse.badRequest(exception.getMessage());
    }
}
