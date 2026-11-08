package ch.grudligstrasse.pickel.adapter.in.rest;

import java.time.LocalDate;

/** Anfrage zum Erfassen oder Ändern eines Milestones; die fachlichen Regeln stehen nur in der Domain. */
public record MilestoneRequest(String name, LocalDate date) {}
