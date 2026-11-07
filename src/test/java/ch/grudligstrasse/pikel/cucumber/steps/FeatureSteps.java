package ch.grudligstrasse.pikel.cucumber.steps;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.withArgs;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FeatureSteps {

    private static final String PI_PATH = "/api/v1/program-increments";

    @Inject
    ScenarioContext context;

    private Response response;

    // Titel → gesendete Angaben; «erscheint im Backlog» prüft, dass sie so gespeichert sind.
    private final Map<String, Map<String, Object>> sent = new HashMap<>();

    @Wenn("ich im PI {string} das Feature {string} mit WSJF-Rang {int}, Owner {string} und Schätzung {int} erfasse")
    public void ichDasFeatureMitAllenAngabenErfasse(String pi, String title, int wsjfRank, String owner, int estimate) {
        // Mit Beschreibung, damit auch ihr Weg durch REST und Datenbank geprüft ist (das Szenario nennt sie nicht).
        erfasse(
                pi,
                Map.of(
                        "title",
                        title,
                        "description",
                        "Kundinnen ändern ihre Adresse selbst",
                        "wsjfRank",
                        wsjfRank,
                        "owner",
                        owner,
                        "estimate",
                        estimate));
    }

    // Die optionalen Felder fehlen ganz im Request, sie werden nicht als null geschickt.
    @Wenn("ich im PI {string} das Feature {string} ohne weitere Angaben erfasse")
    public void ichDasFeatureOhneWeitereAngabenErfasse(String pi, String title) {
        erfasse(pi, Map.of("title", title));
    }

    @Wenn("ich im PI {string} das Feature {string} erfasse")
    public void ichDasFeatureErfasse(String pi, String title) {
        erfasse(pi, Map.of("title", title));
    }

    @Wenn("ich im PI {string} das Feature {string} mit WSJF-Rang {int} erfasse")
    public void ichDasFeatureMitWsjfRangErfasse(String pi, String title, int wsjfRank) {
        erfasse(pi, Map.of("title", title, "wsjfRank", wsjfRank));
    }

    @Wenn("ich im PI {string} das Feature {string} mit Schätzung {int} erfasse")
    public void ichDasFeatureMitSchaetzungErfasse(String pi, String title, int estimate) {
        erfasse(pi, Map.of("title", title, "estimate", estimate));
    }

    @Wenn("ich in einem unbekannten PI das Feature {string} erfasse")
    public void ichInEinemUnbekanntenPiErfasse(String title) {
        post(UUID.randomUUID().toString(), Map.of("title", title));
    }

    @Dann("erscheint {string} im Backlog von {string}")
    public void erscheintImBacklog(String title, String pi) {
        String piId = context.programIncrementId(pi);
        response.then().statusCode(201).body("programIncrementId", is(piId));

        // Jedes Feature im Backlog gehört zu diesem PI: Andere Szenarien erfassen gleichnamige Features in anderen PIs.
        Response backlog = backlog(piId);
        backlog.then().body("title", hasItem(title)).body("programIncrementId", everyItem(is(piId)));
        for (Map.Entry<String, Object> field : sent.get(title).entrySet()) {
            backlog.then().body("find { it.title == '%s' }.%s", withArgs(title, field.getKey()), is(field.getValue()));
        }
    }

    @Dann("ist {string} nicht eingeplant")
    public void istNichtEingeplant(String title) {
        String piId = response.then().statusCode(201).extract().path("programIncrementId");
        response.then().body("scheduled", is(false));
        backlog(piId).then().body("find { it.title == '%s' }.scheduled", withArgs(title), is(false));
    }

    @Dann("zeigt das Backlog von {string} die Features {string}")
    public void zeigtDasBacklogInReihenfolge(String pi, String titles) {
        backlog(context.programIncrementId(pi))
                .then()
                .body(
                        "title",
                        contains(Arrays.stream(titles.split(","))
                                .map(String::trim)
                                .toArray()));
    }

    @Dann("antwortet der Server mit {int} und der Meldung {string}")
    public void antwortetDerServer(int status, String message) {
        response.then().statusCode(status).contentType(ContentType.JSON).body("message", is(message));
    }

    private void erfasse(String pi, Map<String, Object> body) {
        sent.put((String) body.get("title"), body);
        post(context.programIncrementId(pi), body);
    }

    private void post(String piId, Map<String, Object> body) {
        response = given().contentType(ContentType.JSON).body(body).post(featuresPath(piId));
        context.response(response);
    }

    private static Response backlog(String piId) {
        Response backlog = given().get(featuresPath(piId));
        backlog.then().statusCode(200).contentType(ContentType.JSON);
        return backlog;
    }

    private static String featuresPath(String piId) {
        return PI_PATH + "/" + piId + "/features";
    }
}
