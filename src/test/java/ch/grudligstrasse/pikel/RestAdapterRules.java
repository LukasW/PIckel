package ch.grudligstrasse.pikel;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.ws.rs.PathParam;
import java.util.UUID;
import org.jboss.resteasy.reactive.RestPath;

/** Regeln für den REST-Adapter, getrennt von {@link ArchitectureTest}, damit sie an Fixtures prüfbar sind. */
final class RestAdapterRules {

    private RestAdapterRules() {}

    /**
     * Ein {@code UUID}-Pfadparameter scheitert bei einer ungültigen ID an der Umwandlung, bevor ein
     * {@code ExceptionMapper} greift; die Antwort ist dann ein 404 ohne {@code { "message": … }} (ADR 0003). Die
     * Resource nimmt die ID deshalb als {@code String} und behandelt eine ungültige wie eine unbekannte.
     */
    static ArchRule noUuidPathParams() {
        return methods()
                .that()
                .areDeclaredInClassesThat()
                .resideInAPackage("..adapter.in.rest..")
                .should(notHaveUuidPathParameters())
                .because("eine ungültige UUID sonst ohne { \"message\": … } beantwortet wird (ADR 0003)");
    }

    private static ArchCondition<JavaMethod> notHaveUuidPathParameters() {
        return new ArchCondition<>("keine UUID als @PathParam oder @RestPath haben") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                for (JavaParameter parameter : method.getParameters()) {
                    if (isUuidPathParameter(parameter)) {
                        events.add(SimpleConditionEvent.violated(
                                method,
                                "%s hat Parameter %d vom Typ UUID als Pfadparameter"
                                        .formatted(method.getFullName(), parameter.getIndex())));
                    }
                }
            }
        };
    }

    private static boolean isUuidPathParameter(JavaParameter parameter) {
        return parameter.getRawType().isEquivalentTo(UUID.class)
                && (parameter.isAnnotatedWith(PathParam.class) || parameter.isAnnotatedWith(RestPath.class));
    }
}
