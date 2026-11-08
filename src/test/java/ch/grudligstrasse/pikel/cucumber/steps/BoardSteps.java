package ch.grudligstrasse.pikel.cucumber.steps;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.withArgs;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.UUID;

/** US-12: das Program Board über {@code GET /api/v1/program-increments/{id}/board}. */
public class BoardSteps {

    private static final String PI_PATH = "/api/v1/program-increments";

    // «antwortet der Server mit …» (FeatureSteps) prüft die letzte Antwort im gemeinsamen Kontext.
    @Inject
    ScenarioContext context;

    private Response board;

    @Wenn("ich das Board von {string} abrufe")
    public void ichDasBoardAbrufe(String programIncrement) {
        get(context.programIncrementId(programIncrement));
    }

    @Wenn("ich das Board eines unbekannten PI abrufe")
    public void ichDasBoardEinesUnbekanntenPiAbrufe() {
        get(UUID.randomUUID().toString());
    }

    // contains: genau diese Zeilen in dieser Reihenfolge.
    @Dann("hat das Board die Zeilen {string}, {string}")
    public void hatDasBoardDieZeilen(String first, String second) {
        ok().then().body("teams.name", contains(first, second));
    }

    @Dann("hat das Board {int} Spalten, die letzte ist {string}")
    public void hatDasBoardSpalten(int count, String last) {
        ok().then()
                .body("iterations", hasSize(count))
                .body("iterations[-1].name", is(last))
                .body("iterations[-1].innovationAndPlanning", is(true));
    }

    // US-17: Die Richtung zählt; vom brauchenden Feature aus genau diese eine Lieferung.
    @Dann("liefert das Board die Abhängigkeit {string} braucht {string}")
    public void liefertDasBoardDieAbhaengigkeit(String needing, String needed) {
        ok().then()
                .body(
                        "dependencies.findAll { it.needingFeatureId == '%s' }.neededFeatureId",
                        withArgs(context.featureId(needing)), contains(context.featureId(needed)));
    }

    // Liest das Board des PI, zu dem das Feature gehört, frisch: Nach einer Änderung (US-13: zurück ins Backlog, Team
    // entfernt) hat das Szenario das Board nicht eigens abgerufen. Im Backlog heisst auch: nicht eingeplant.
    @Dann("liegt {string} im Backlog des Boards")
    public void liegtImBacklogDesBoards(String title) {
        Response current = given().get(PI_PATH + "/" + context.featureProgramIncrementId(title) + "/board");
        current.then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("backlog.title", hasItem(title))
                .body("scheduled.title", not(hasItem(title)));
    }

    private void get(String piId) {
        board = given().get(PI_PATH + "/" + piId + "/board");
        context.response(board);
    }

    private Response ok() {
        board.then().statusCode(200).contentType(ContentType.JSON);
        return board;
    }
}
