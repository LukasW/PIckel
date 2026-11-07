package ch.grudligstrasse.pikel.adapter.in.rest;

import ch.grudligstrasse.pikel.domain.model.PiLinkNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Unbekannter Link am PI → 404 mit Meldung für die Oberfläche. */
@Provider
public class PiLinkNotFoundExceptionMapper implements ExceptionMapper<PiLinkNotFoundException> {

    @Override
    public Response toResponse(PiLinkNotFoundException exception) {
        return ErrorResponse.of(Response.Status.NOT_FOUND, exception.getMessage());
    }
}
