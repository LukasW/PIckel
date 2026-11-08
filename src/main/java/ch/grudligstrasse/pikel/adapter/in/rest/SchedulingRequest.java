package ch.grudligstrasse.pikel.adapter.in.rest;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.UUID;

/** Angaben zum Einplanen: Team und Nummer der Iteration im PI; die fachlichen Regeln stehen nur in der Domain. */
public record SchedulingRequest(
        UUID teamId,

        @JsonDeserialize(using = WholeNumberDeserializer.class)
        Integer iterationNumber) {}
