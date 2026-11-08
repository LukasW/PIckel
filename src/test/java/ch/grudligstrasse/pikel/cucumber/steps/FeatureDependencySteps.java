package ch.grudligstrasse.pikel.cucumber.steps;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.withArgs;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Gegebensei;
import io.cucumber.java.de.Wenn;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.Map;

/**
 * US-16: Abhängigkeiten über {@code /api/v1/program-increments/{piId}/dependencies}. Die Features legt FeatureSteps an
 * und meldet sie im gemeinsamen Kontext; «wird das Erfassen … abgelehnt» (ScenarioContext) prüft die Ablehnung, «ich
 * das Feature … lösche» (FeatureSteps) das Löschen samt Abhängigkeiten.
 */
public class FeatureDependencySteps {

    private static final String PI_PATH = "/api/v1/program-increments";

    @Inject
    ScenarioContext context;

    private Response added;

    @Gegebensei("{string} braucht {string}")
    public void braucht(String needing, String needed) {
        festhalten(needing, needed).then().statusCode(201);
    }

    // Das PI der URL ist das PI des brauchenden Features; so prüft der Server das gebrauchte Feature dagegen.
    @Wenn("ich festhalte, dass {string} das Feature {string} braucht")
    public void ichFesthalte(String needing, String needed) {
        added = festhalten(needing, needed);
        context.response(added);
    }

    @Wenn("ich die Abhängigkeit von {string} zu {string} entferne")
    public void ichDieAbhaengigkeitEntferne(String needing, String needed) {
        String path = dependenciesPath(context.featureProgramIncrementId(needing)) + "/" + context.featureId(needing)
                + "/" + context.featureId(needed);
        context.response(given().delete(path));
    }

    // Prüft die Antwort des POST (201, Richtung wie gesendet) und den gespeicherten Stand.
    @Dann("braucht {string} das Feature {string}")
    public void brauchtDasFeature(String needing, String needed) {
        String needingId = context.featureId(needing);
        String neededId = context.featureId(needed);
        added.then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("needingFeatureId", is(needingId))
                .body("neededFeatureId", is(neededId));
        list(context.featureProgramIncrementId(needing))
                .then()
                .body(
                        "findAll { it.needingFeatureId == '%s' }.neededFeatureId",
                        withArgs(needingId), hasItem(neededId));
    }

    // Dieselbe Abhängigkeit von der Lieferung aus gesehen: im PI des gebrauchten Features, gesucht über seine ID.
    @Dann("wird {string} von {string} gebraucht")
    public void wirdGebraucht(String needed, String needing) {
        String neededId = context.featureId(needed);
        list(context.featureProgramIncrementId(needed))
                .then()
                .body(
                        "findAll { it.neededFeatureId == '%s' }.needingFeatureId",
                        withArgs(neededId), hasItem(context.featureId(needing)));
    }

    // Nach dem Entfernen (204) wie nach dem Löschen des gebrauchten Features (204): keine Abhängigkeit mehr mit
    // dem Feature als Bedarf.
    @Dann("braucht {string} kein Feature")
    public void brauchtKeinFeature(String needing) {
        context.lastResponse().then().statusCode(204);
        list(context.featureProgramIncrementId(needing))
                .then()
                .body("needingFeatureId", not(hasItem(context.featureId(needing))));
    }

    // Nach dem Löschen des brauchenden Features (204): keine Abhängigkeit mehr mit dem Feature als Lieferung.
    @Dann("wird {string} von keinem Feature gebraucht")
    public void wirdVonKeinemGebraucht(String needed) {
        context.lastResponse().then().statusCode(204);
        list(context.featureProgramIncrementId(needed))
                .then()
                .body("neededFeatureId", not(hasItem(context.featureId(needed))));
    }

    private Response festhalten(String needing, String needed) {
        Map<String, String> body =
                Map.of("needingFeatureId", context.featureId(needing), "neededFeatureId", context.featureId(needed));
        return given().contentType(ContentType.JSON)
                .body(body)
                .post(dependenciesPath(context.featureProgramIncrementId(needing)));
    }

    private static Response list(String piId) {
        Response list = given().get(dependenciesPath(piId));
        list.then().statusCode(200).contentType(ContentType.JSON);
        return list;
    }

    private static String dependenciesPath(String piId) {
        return PI_PATH + "/" + piId + "/dependencies";
    }
}
