package ch.grudligstrasse.pikel.adapter.in.rest;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

/**
 * Nimmt nur eine ganze JSON-Zahl an. Jackson kürzt {@code 1.5} sonst still auf {@code 1}; dann speicherte PIkel einen
 * Wert, den niemand erfasst hat. Pro Feld statt global, damit bestehende Felder ihr Verhalten behalten. Die
 * Ablehnung landet als {@code MismatchedInputException} im {@link MismatchedInputExceptionMapper}.
 */
public class WholeNumberDeserializer extends StdDeserializer<Integer> {

    public WholeNumberDeserializer() {
        super(Integer.class);
    }

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
            return (Integer) context.handleUnexpectedToken(Integer.class, parser);
        }
        return parser.getIntValue();
    }
}
