package ch.grudligstrasse.pikel.adapter.in.rest;

import ch.grudligstrasse.pikel.domain.model.InvalidCapacityException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Fachliche Regelverletzung bei der Kapazität → 400 mit Meldung für die Oberfläche. */
@Provider
public class InvalidCapacityExceptionMapper implements ExceptionMapper<InvalidCapacityException> {

    @Override
    public Response toResponse(InvalidCapacityException exception) {
        return ErrorResponse.badRequest(exception.getMessage());
    }
}
