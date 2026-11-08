package ch.grudligstrasse.pikel.cucumber.steps;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
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

    // US-14: Team und Iteration über die Zeilen und Spalten desselben Boards. contains: genau eine Zelle mit diesen
    // Punkten, auch ohne Eintrag (dann 0).
    @Dann("liefert das Board für {string} in {string} die Kapazität {int}")
    public void liefertDasBoardDieKapazitaet(String team, String iteration, int points) {
        JsonPath json = ok().jsonPath();
        String teamId = json.param("name", team).getString("teams.find { it.name == name }.id");
        int number = json.param("name", iteration).getInt("iterations.find { it.name == name }.number");

        board.then()
                .body(
                        "capacities.findAll { it.teamId == '%s' && it.iterationNumber == %d }.points"
                                .formatted(teamId, number),
                        contains(points));
    }

    // In der IP-Iteration wird keine Kapazität geplant (ADR 0006): keine Zelle, für kein Team.
    @Dann("liefert das Board für die IP-Iteration keine Kapazität")
    public void liefertDasBoardFuerDieIpIterationKeineKapazitaet() {
        int ipNumber = ok().jsonPath().getInt("iterations.find { it.innovationAndPlanning }.number");

        board.then()
                .body("capacities.findAll { it.iterationNumber == %d }".formatted(ipNumber), empty())
                .body("capacities", not(empty()));
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
