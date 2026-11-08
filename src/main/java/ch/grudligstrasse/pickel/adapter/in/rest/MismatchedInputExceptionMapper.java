package ch.grudligstrasse.pickel.adapter.in.rest;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Gültiges JSON mit einem Wert, der nicht zum Feld passt (z. B. Datum «2027-13-45») → 400 im einheitlichen
 * Fehlerkörper. Ersetzt den eingebauten Mapper von Quarkus, der eine andere Form liefert.
 */
@Provider
public class MismatchedInputExceptionMapper implements ExceptionMapper<MismatchedInputException> {

    static final String MESSAGE = "Die Anfrage enthält einen ungültigen Wert.";

    @Override
    public Response toResponse(MismatchedInputException exception) {
        return ErrorResponse.badRequest(MESSAGE);
    }
}
