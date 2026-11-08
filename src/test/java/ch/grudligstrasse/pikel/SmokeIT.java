package ch.grudligstrasse.pikel;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Startet das gepackte JAR (Quinoa-Bundle, Liquibase, Hibernate-Validate) und prüft die Readiness.
 * Läuft im PR-Workflow, damit Boot-Fehler nicht erst auf main auffallen.
 */
@QuarkusIntegrationTest
class SmokeIT {

    @Test
    void readinessProbeReportsUp() {
        given().when().get("/q/health/ready").then().statusCode(200).body("status", is("UP"));
    }

    @Test
    void servesAngularApp() {
        given().when().get("/").then().statusCode(200);
    }

    /**
     * #36: Ein Reload oder ein Lesezeichen auf eine Unterseite muss die SPA ausliefern, damit der Angular-Router die
     * Route auflöst. Die UUID gehört zu keinem PI; aufgelöst wird sie erst im Browser.
     */
    @ParameterizedTest
    @ValueSource(strings = {"/teams", "/program-increments/3f2b8c1e-7d4a-4e9b-9c11-5a6f0d2e8b47/board"})
    void directCallOfSpaRouteServesAngularApp(String route) {
        given().accept("text/html")
                .when()
                .get(route)
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"))
                .body(containsString("<app-root>"));
    }
}
