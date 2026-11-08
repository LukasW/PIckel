package ch.grudligstrasse.pickel.adapter.in.rest;

/** Anfrage für Business-Kontext und PI Objectives des ART; ein fehlendes Feld gilt als leerer Text. */
public record UpdateBusinessContextRequest(String businessContext, String artObjectives) {}
