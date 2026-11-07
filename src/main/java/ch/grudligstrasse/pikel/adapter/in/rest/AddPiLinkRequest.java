package ch.grudligstrasse.pikel.adapter.in.rest;

/** Anfrage für einen neuen Link am PI; die Regeln stehen nur in der Domain. */
public record AddPiLinkRequest(String title, String url) {}
