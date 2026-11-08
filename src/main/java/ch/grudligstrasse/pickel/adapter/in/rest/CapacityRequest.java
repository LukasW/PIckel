package ch.grudligstrasse.pickel.adapter.in.rest;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/** Anfrage zum Erfassen einer Kapazität; die fachlichen Regeln stehen nur in der Domain. */
public record CapacityRequest(
        @JsonDeserialize(using = WholeNumberDeserializer.class)
        Integer points) {}
