package ch.grudligstrasse.pikel.adapter.in.rest;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Anfrage ohne Inhalt → 400 im einheitlichen Fehlerkörper statt des Violation-Reports von Quarkus. */
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    static final String MESSAGE = "Die Anfrage ist unvollständig.";

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        return ErrorResponse.badRequest(MESSAGE);
    }
}
