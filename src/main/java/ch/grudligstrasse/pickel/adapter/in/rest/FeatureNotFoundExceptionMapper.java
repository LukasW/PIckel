package ch.grudligstrasse.pickel.adapter.in.rest;

import ch.grudligstrasse.pickel.domain.model.FeatureNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Unbekanntes Feature → 404 mit Meldung für die Oberfläche. */
@Provider
public class FeatureNotFoundExceptionMapper implements ExceptionMapper<FeatureNotFoundException> {

    @Override
    public Response toResponse(FeatureNotFoundException exception) {
        return ErrorResponse.of(Response.Status.NOT_FOUND, exception.getMessage());
    }
}
