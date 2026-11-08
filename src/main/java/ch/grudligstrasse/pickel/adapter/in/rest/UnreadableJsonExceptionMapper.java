package ch.grudligstrasse.pickel.adapter.in.rest;

import com.fasterxml.jackson.core.exc.StreamReadException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Kein gültiges JSON → 400 im einheitlichen Fehlerkörper. Quarkus verpackt den Parse-Fehler in eine
 * {@link WebApplicationException}; alle anderen behalten ihre Antwort (404, 405, …).
 */
@Provider
public class UnreadableJsonExceptionMapper implements ExceptionMapper<WebApplicationException> {

    static final String MESSAGE = "Die Anfrage ist kein gültiges JSON.";

    @Override
    public Response toResponse(WebApplicationException exception) {
        if (exception.getCause() instanceof StreamReadException) {
            return ErrorResponse.badRequest(MESSAGE);
        }
        return exception.getResponse();
    }
}
