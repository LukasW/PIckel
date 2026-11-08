package ch.grudligstrasse.pickel.adapter.in.rest;

/** Anfrage zum Erfassen eines Features; die Regeln stehen nur in der Domain. Optionale Angaben dürfen fehlen. */
public record FeatureRequest(String title, String description, Integer wsjfRank, String owner, Integer estimate) {}
