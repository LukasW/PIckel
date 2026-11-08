package ch.grudligstrasse.pikel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.grudligstrasse.pikel.archfixture.adapter.in.rest.StringPathParamResource;
import ch.grudligstrasse.pikel.archfixture.adapter.in.rest.UnannotatedUuidResource;
import ch.grudligstrasse.pikel.archfixture.adapter.in.rest.UuidPathParamResource;
import ch.grudligstrasse.pikel.archfixture.adapter.in.rest.UuidRestPathResource;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.api.Test;

/**
 * Weist nach, dass {@link RestAdapterRules#noUuidPathParams()} am Verstoss anschlägt; ohne diesen Nachweis wäre eine
 * Regel, die nie anschlägt, in {@link ArchitectureTest} nicht von einer funktionierenden zu unterscheiden. Die
 * Fixtures liegen ausserhalb des Produktionspakets, damit {@link ArchitectureTest} sie nicht sieht.
 */
class RestAdapterRulesTest {

    @Test
    void flagsUuidAnnotatedWithPathParam() {
        assertTrue(evaluate(UuidPathParamResource.class).hasViolation());
    }

    @Test
    void flagsUuidAnnotatedWithRestPath() {
        assertTrue(evaluate(UuidRestPathResource.class).hasViolation());
    }

    @Test
    void allowsStringAnnotatedWithPathParam() {
        assertFalse(evaluate(StringPathParamResource.class).hasViolation());
    }

    @Test
    void allowsUuidWithoutPathAnnotation() {
        assertFalse(evaluate(UnannotatedUuidResource.class).hasViolation());
    }

    private static EvaluationResult evaluate(Class<?> fixture) {
        JavaClasses classes = new ClassFileImporter().importClasses(fixture);
        return RestAdapterRules.noUuidPathParams().evaluate(classes);
    }
}
