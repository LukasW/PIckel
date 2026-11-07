package ch.grudligstrasse.pikel.adapter.in.rest;

import ch.grudligstrasse.pikel.domain.model.InvalidFeatureException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Fachliche Regelverletzung bei Features → 400 mit Meldung für die Oberfläche. */
@Provider
public class InvalidFeatureExceptionMapper implements ExceptionMapper<InvalidFeatureException> {

    @Override
    public Response toResponse(InvalidFeatureException exception) {
        return ErrorResponse.badRequest(exception.getMessage());
    }
}
