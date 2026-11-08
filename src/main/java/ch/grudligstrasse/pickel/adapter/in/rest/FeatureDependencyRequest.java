package ch.grudligstrasse.pickel.adapter.in.rest;

import java.util.UUID;

/** Angaben zum Erfassen einer Abhängigkeit: das brauchende und das gebrauchte Feature. */
public record FeatureDependencyRequest(UUID needingFeatureId, UUID neededFeatureId) {}
