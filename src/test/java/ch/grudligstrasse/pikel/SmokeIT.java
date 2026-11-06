package ch.grudligstrasse.pikel;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;

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
}
