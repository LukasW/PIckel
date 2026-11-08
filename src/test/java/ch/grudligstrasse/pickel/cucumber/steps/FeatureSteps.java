package ch.grudligstrasse.pickel.cucumber.steps;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.withArgs;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Gegebensei;
import io.cucumber.java.de.Wenn;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeatureSteps {

    private static final String PI_PATH = "/api/v1/program-increments";

    @Inject
    ScenarioContext context;

    private Response response;

    // Titel → gesendete Angaben; «erscheint im Backlog» prüft, dass sie so gespeichert sind.
    private final Map<String, Map<String, Object>> sent = new HashMap<>();

    private static final Pattern QUOTED = Pattern.compile("\"([^\"]*)\"");

    // Titel → ID bzw. PI-ID der Features, die das Szenario als gegeben angelegt hat (US-10).
    private final Map<String, String> ids = new HashMap<>();
    private final Map<String, String> piIds = new HashMap<>();

    // Titel des Features, das zuletzt bearbeitet oder gelöscht wurde.
    private String edited;

    // Antwort des Löschens im Szenario; fehlt sie, kommt «ist das Backlog leer» nach einem Import (US-11).
    private Response deletion;

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

    // Auch nach einem CSV-Import (US-11, FeatureImportSteps): Dann prüft der Step nur das Backlog, weil die Antwort
    // des Imports kein Feature ist und die Angaben in der Datei stehen; die Antwort prüft «wurde … importiert».
    @Dann("erscheint {string} im Backlog von {string}")
    public void erscheintImBacklog(String title, String pi) {
        String piId = context.programIncrementId(pi);
        context.lastResponse().then().statusCode(201);

        // Jedes Feature im Backlog gehört zu diesem PI: Andere Szenarien erfassen gleichnamige Features in anderen PIs.
        Response backlog = backlog(piId);
        backlog.then().body("title", hasItem(title)).body("programIncrementId", everyItem(is(piId)));
        if (!sent.containsKey(title)) {
            return;
        }
        response.then().statusCode(201).body("programIncrementId", is(piId));
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

    // Über den gemeinsamen Kontext: Auch das Board (US-12, BoardSteps) antwortet so auf ein unbekanntes PI.
    @Dann("antwortet der Server mit {int} und der Meldung {string}")
    public void antwortetDerServer(int status, String message) {
        context.lastResponse()
                .then()
                .statusCode(status)
                .contentType(ContentType.JSON)
                .body("message", is(message));
    }

    // --- US-10: Backlog bearbeiten ---

    @Gegebensei("im PI {string} das Feature {string} ohne WSJF-Rang")
    public void dasFeatureOhneWsjfRang(String pi, String title) {
        vorhanden(pi, Map.of("title", title));
    }

    @Gegebensei("im PI {string} das Feature {string} mit WSJF-Rang {int}")
    public void dasFeatureMitWsjfRang(String pi, String title, int wsjfRank) {
        vorhanden(pi, Map.of("title", title, "wsjfRank", wsjfRank));
    }

    // Die Anzahl der Titel wechselt je Szenario; darum ein regulärer Ausdruck statt {string} je Titel.
    @Dann("^zeigt das Backlog von \"([^\"]*)\" die Features ((?:\"[^\"]*\"(?:, )?)+) in dieser Reihenfolge$")
    public void zeigtDasBacklogInDieserReihenfolge(String pi, String titles) {
        List<String> expected = new ArrayList<>();
        Matcher quoted = QUOTED.matcher(titles);
        while (quoted.find()) {
            expected.add(quoted.group(1));
        }
        backlog(context.programIncrementId(pi)).then().body("title", contains(expected.toArray()));
    }

    @Wenn("ich beim Feature {string} den WSJF-Rang auf {int} und die Schätzung auf {int} setze")
    public void ichBeimFeatureRangUndSchaetzungSetze(String title, int wsjfRank, int estimate) {
        Map<String, Object> body = current(title);
        body.put("wsjfRank", wsjfRank);
        body.put("estimate", estimate);
        bearbeite(title, body);
    }

    @Wenn("ich beim Feature {string} den Titel auf {string} setze")
    public void ichBeimFeatureDenTitelSetze(String title, String newTitle) {
        Map<String, Object> body = current(title);
        body.put("title", newTitle);
        bearbeite(title, body);
    }

    @Wenn("ich im PI {string} das Feature mit der ID {string} bearbeite")
    public void ichDasFeatureMitDerIdBearbeite(String pi, String id) {
        response = given().contentType(ContentType.JSON)
                .body(Map.of("title", "Login"))
                .put(featurePath(context.programIncrementId(pi), id));
        context.response(response);
    }

    @Wenn("ich das Feature {string} lösche")
    public void ichDasFeatureLoesche(String title) {
        edited = title;
        response = given().delete(featurePath(piOf(title), idOf(title)));
        context.response(response);
        deletion = response;
    }

    @Wenn("ich im PI {string} das Feature mit der ID {string} lösche")
    public void ichDasFeatureMitDerIdLoesche(String pi, String id) {
        response = given().delete(featurePath(context.programIncrementId(pi), id));
        context.response(response);
    }

    @Wenn("ich das Feature {string} im PI {string} bearbeite")
    public void ichDasFeatureImPiBearbeite(String title, String pi) {
        Map<String, Object> body = current(title);
        body.put("title", "Anmeldung");
        response = given().contentType(ContentType.JSON)
                .body(body)
                .put(featurePath(context.programIncrementId(pi), idOf(title)));
        context.response(response);
    }

    @Wenn("ich das Feature {string} im PI {string} lösche")
    public void ichDasFeatureImPiLoesche(String title, String pi) {
        response = given().delete(featurePath(context.programIncrementId(pi), idOf(title)));
        context.response(response);
    }

    // Prüft die Antwort des PUT (200, dasselbe Feature) und den gespeicherten Stand im Backlog.
    @Dann("hat {string} den WSJF-Rang {int} und die Schätzung {int}")
    public void hatDenRangUndDieSchaetzung(String title, int wsjfRank, int estimate) {
        response.then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", is(idOf(title)))
                .body("title", is(title))
                .body("wsjfRank", is(wsjfRank))
                .body("estimate", is(estimate));
        backlog(piOf(title))
                .then()
                .body("find { it.id == '%s' }.title", withArgs(idOf(title)), is(title))
                .body("find { it.id == '%s' }.wsjfRank", withArgs(idOf(title)), is(wsjfRank))
                .body("find { it.id == '%s' }.estimate", withArgs(idOf(title)), is(estimate));
    }

    // Abgelehnt heisst auch: Das Feature steht unverändert im Backlog.
    @Dann("wird das Bearbeiten mit der Meldung {string} abgelehnt")
    public void wirdDasBearbeitenAbgelehnt(String message) {
        response.then().statusCode(400).contentType(ContentType.JSON).body("message", is(message));
        backlog(piOf(edited)).then().body("find { it.id == '%s' }.title", withArgs(idOf(edited)), is(edited));
    }

    // Nach dem Löschen (US-10) auch die Antwort 204; nach einem abgelehnten Import (US-11) hat «wird der Import …
    // abgelehnt» die Antwort schon geprüft, hier zählt nur, dass nichts gespeichert wurde.
    @Dann("ist das Backlog von {string} leer")
    public void istDasBacklogLeer(String pi) {
        if (deletion != null) {
            deletion.then().statusCode(204);
        }
        backlog(context.programIncrementId(pi)).then().body("$", is(empty()));
    }

    @Dann("erscheint {string} weiterhin im Backlog von {string}")
    public void erscheintWeiterhinImBacklog(String title, String pi) {
        backlog(context.programIncrementId(pi)).then().body("id", hasItem(idOf(title)));
    }

    private void vorhanden(String pi, Map<String, Object> body) {
        String piId = context.programIncrementId(pi);
        post(piId, body);
        String id = response.then().statusCode(201).extract().path("id");
        String title = (String) body.get("title");
        ids.put(title, id);
        piIds.put(title, piId);
        context.featureCreated(title, id, piId);
    }

    // PUT ersetzt alle Angaben; der Step schickt darum den gespeicherten Stand mit, nur die genannten Felder neu.
    private Map<String, Object> current(String title) {
        Map<String, Object> stored =
                backlog(piOf(title)).then().extract().path("find { it.id == '%s' }".formatted(idOf(title)));
        Map<String, Object> body = new HashMap<>();
        for (String field : List.of("title", "description", "wsjfRank", "owner", "estimate")) {
            body.put(field, stored.get(field));
        }
        return body;
    }

    private void bearbeite(String title, Map<String, Object> body) {
        edited = title;
        response = given().contentType(ContentType.JSON).body(body).put(featurePath(piOf(title), idOf(title)));
        context.response(response);
    }

    private String idOf(String title) {
        String id = ids.get(title);
        if (id == null) {
            throw new IllegalStateException("Das Szenario hat kein Feature \"" + title + "\" angelegt.");
        }
        return id;
    }

    private String piOf(String title) {
        idOf(title);
        return piIds.get(title);
    }

    private static String featurePath(String piId, String id) {
        return featuresPath(piId) + "/" + id;
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
