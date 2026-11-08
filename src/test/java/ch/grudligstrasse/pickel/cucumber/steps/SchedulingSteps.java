package ch.grudligstrasse.pickel.cucumber.steps;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * US-13: Einplanen über {@code PUT}/{@code DELETE …/features/{id}/scheduling}; geprüft wird am Board
 * ({@code GET …/board}). Iterationen nennt das Szenario beim Namen, die Schnittstelle bei der Nummer; die Nummer kommt
 * darum aus den Spalten des Boards, nicht aus einer Rechnung im Test.
 */
public class SchedulingSteps {

    private static final String PI_PATH = "/api/v1/program-increments";
    private static final String TEAMS_PATH = "/api/v1/teams";

    // Features (FeatureSteps) und Ablehnungen («antwortet der Server mit …») teilen sich den Kontext.
    @Inject
    ScenarioContext context;

    // Titel des Features, das zuletzt eingeplant werden sollte; eine Ablehnung prüft, dass es im Backlog bleibt.
    private String scheduling;

    @Gegebensei("im PI {string} das Feature {string} bei {string} in {string} eingeplant")
    public void dasFeatureEingeplant(String pi, String title, String team, String iteration) {
        String piId = context.programIncrementId(pi);
        String id = given().contentType(ContentType.JSON)
                .body(Map.of("title", title))
                .post(featuresPath(piId))
                .then()
                .statusCode(201)
                .extract()
                .path("id");
        context.featureCreated(title, id, piId);
        ichEinplane(title, team, iteration);
        context.lastResponse().then().statusCode(200);
    }

    @Wenn("ich {string} bei {string} in {string} einplane")
    public void ichEinplane(String title, String team, String iteration) {
        schedule(title, Map.of("teamId", teamId(team), "iterationNumber", iterationNumber(title, iteration)));
    }

    @Wenn("ich {string} bei {string} in die Iteration Nummer {int} einplane")
    public void ichInDieIterationNummerEinplane(String title, String team, int number) {
        schedule(title, Map.of("teamId", teamId(team), "iterationNumber", number));
    }

    @Wenn("ich {string} bei einem unbekannten Team in {string} einplane")
    public void ichBeiEinemUnbekanntenTeamEinplane(String title, String iteration) {
        schedule(
                title,
                Map.of("teamId", UUID.randomUUID().toString(), "iterationNumber", iterationNumber(title, iteration)));
    }

    @Wenn("ich {string} ohne Team und Iteration einplane")
    public void ichOhneAngabenEinplane(String title) {
        schedule(title, Map.of());
    }

    @Wenn("ich {string} zurück ins Backlog nehme")
    public void ichZurueckInsBacklogNehme(String title) {
        context.response(given().delete(schedulingPath(title)));
    }

    // PUT ersetzt alle Angaben; der Step schickt darum den gespeicherten Stand mit, nur der Titel ist neu. Die
    // Einplanung gehört nicht zum Body des Bearbeitens (Plan: eigene Unterressource).
    @Wenn("ich {string} in {string} umbenenne")
    public void ichUmbenenne(String title, String newTitle) {
        String piId = context.featureProgramIncrementId(title);
        String id = context.featureId(title);
        Map<String, Object> stored = given().get(featuresPath(piId))
                .then()
                .statusCode(200)
                .extract()
                .path("find { it.id == '%s' }".formatted(id));
        Map<String, Object> body = new HashMap<>();
        for (String field : List.of("description", "wsjfRank", "owner", "estimate")) {
            body.put(field, stored.get(field));
        }
        body.put("title", newTitle);
        context.response(given().contentType(ContentType.JSON).body(body).put(featuresPath(piId) + "/" + id));
        context.featureCreated(newTitle, id, piId);
    }

    // Die Antwort der Änderung (Einplanen oder Bearbeiten) nennt die Zelle, das Board zeigt sie danach auch.
    @Dann("liegt {string} auf dem Board bei {string} in {string}")
    public void liegtAufDemBoardBei(String title, String team, String iteration) {
        String id = context.featureId(title);
        String teamId = teamId(team);
        int number = iterationNumber(title, iteration);
        context.lastResponse()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", is(id))
                .body("title", is(title))
                .body("scheduled", is(true))
                .body("teamId", is(teamId))
                .body("iterationNumber", is(number));
        board(title)
                .then()
                .body("scheduled.find { it.id == '%s' }.title", withArgs(id), is(title))
                .body("scheduled.find { it.id == '%s' }.teamId", withArgs(id), is(teamId))
                .body("scheduled.find { it.id == '%s' }.iterationNumber", withArgs(id), is(number))
                .body("backlog.id", not(hasItem(id)));
    }

    @Dann("liegt {string} nicht mehr im Backlog des Boards")
    public void liegtNichtMehrImBacklog(String title) {
        board(title).then().body("backlog.id", not(hasItem(context.featureId(title))));
    }

    // Abgelehnt heisst auch: Das Feature bleibt im Backlog, nichts ist eingeplant.
    @Dann("wird das Einplanen mit der Meldung {string} abgelehnt")
    public void wirdDasEinplanenAbgelehnt(String message) {
        context.lastResponse()
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("message", is(message));
        String id = context.featureId(scheduling);
        board(scheduling).then().body("backlog.id", hasItem(id)).body("scheduled.id", not(hasItem(id)));
    }

    private void schedule(String title, Map<String, Object> body) {
        scheduling = title;
        context.response(given().contentType(ContentType.JSON).body(body).put(schedulingPath(title)));
    }

    private String schedulingPath(String title) {
        return featuresPath(context.featureProgramIncrementId(title)) + "/" + context.featureId(title) + "/scheduling";
    }

    // Teams sind eindeutig beim Namen (US-6); die Grundlage leert die Liste.
    private static String teamId(String name) {
        String id = given().get(TEAMS_PATH)
                .then()
                .statusCode(200)
                .extract()
                .path("find { it.name == '%s' }.id".formatted(name));
        if (id == null) {
            throw new IllegalStateException("Es gibt kein Team \"" + name + "\".");
        }
        return id;
    }

    private int iterationNumber(String title, String iterationName) {
        Integer number = board(title)
                .then()
                .extract()
                .path("iterations.find { it.name == '%s' }.number".formatted(iterationName));
        if (number == null) {
            throw new IllegalStateException("Das Board hat keine Spalte \"" + iterationName + "\".");
        }
        return number;
    }

    private Response board(String title) {
        Response board = given().get(PI_PATH + "/" + context.featureProgramIncrementId(title) + "/board");
        board.then().statusCode(200).contentType(ContentType.JSON);
        return board;
    }

    private static String featuresPath(String piId) {
        return PI_PATH + "/" + piId + "/features";
    }
}
