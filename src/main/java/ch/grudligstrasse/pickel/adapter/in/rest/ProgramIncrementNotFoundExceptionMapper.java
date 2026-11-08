package ch.grudligstrasse.pickel.adapter.in.rest;

import ch.grudligstrasse.pickel.domain.model.ProgramIncrementNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Unbekanntes PI → 404 mit Meldung für die Oberfläche. */
@Provider
public class ProgramIncrementNotFoundExceptionMapper implements ExceptionMapper<ProgramIncrementNotFoundException> {

    @Override
    public Response toResponse(ProgramIncrementNotFoundException exception) {
        return ErrorResponse.of(Response.Status.NOT_FOUND, exception.getMessage());
    }
}
