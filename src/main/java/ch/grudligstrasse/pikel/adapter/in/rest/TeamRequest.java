package ch.grudligstrasse.pikel.adapter.in.rest;

/** Anfrage zum Erfassen oder Umbenennen eines Teams; die Regeln stehen nur in der Domain. */
public record TeamRequest(String name) {}
