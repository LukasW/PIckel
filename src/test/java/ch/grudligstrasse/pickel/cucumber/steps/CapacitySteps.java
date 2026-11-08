package ch.grudligstrasse.pickel.cucumber.steps;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;

import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Gegebensei;
import io.cucumber.java.de.Wenn;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** US-7: Kapazität pro Team und Iteration über {@code /api/v1/program-increments/{id}/capacities}. */
public class CapacitySteps {

    private static final String PI_PATH = "/api/v1/program-increments";
    private static final String TEAM_PATH = "/api/v1/teams";
    private static final int FIRST_ITERATION = 1;

    // Die Ablehnungs-Steps («wird das Erfassen … abgelehnt») liegen gemeinsam mit Teams und Features dort.
    @Inject
    ScenarioContext context;

    // US-14: Das PI kommt aus dem Szenario (Namen sind nicht eindeutig). Teams gelten für alle PIs; gibt es "Adler"
    // aus einem anderen Szenario schon, nimmt der Step dieses Team, sonst scheiterte das Anlegen am Namen.
    @Gegebensei("das Team {string} mit der Kapazität {int} in {string} von {string}")
    public void dasTeamMitDerKapazitaet(String team, int points, String iteration, String programIncrement) {
        String piId = context.programIncrementId(programIncrement);
        String teamId = existingTeamIdOf(team).orElseGet(() -> createTeam(team));
        put(piId, teamId, iterationNumberOf(piId, iteration), String.valueOf(points));
        context.lastResponse().then().statusCode(200);
    }

    // {} statt {int}: Derselbe Step schickt auch 1.5 roh als JSON-Zahl, ohne einen zweiten, fast gleichen Step-Text.
    @Wenn("ich für {string} in {string} von {string} die Kapazität {} erfasse")
    public void ichDieKapazitaetErfasse(String team, String iteration, String programIncrement, String points) {
        String piId = piIdOf(programIncrement);
        put(piId, teamIdOf(team), iterationNumberOf(piId, iteration), points);
    }

    @Wenn("ich für ein unbekanntes Team in {string} von {string} die Kapazität {} erfasse")
    public void ichDieKapazitaetEinesUnbekanntenTeamsErfasse(String iteration, String programIncrement, String points) {
        String piId = piIdOf(programIncrement);
        put(piId, UUID.randomUUID().toString(), iterationNumberOf(piId, iteration), points);
    }

    @Wenn("ich für {string} in der ersten Iteration eines unbekannten PI die Kapazität {} erfasse")
    public void ichDieKapazitaetInEinemUnbekanntenPiErfasse(String team, String points) {
        put(UUID.randomUUID().toString(), teamIdOf(team), FIRST_ITERATION, points);
    }

    // Prüft den gespeicherten Stand über GET: Die Zelle muss in der Matrix stehen, auch ohne Eintrag (dann 0).
    @Dann("hat {string} in {string} von {string} die Kapazität {int}")
    public void hatDieKapazitaet(String team, String iteration, String programIncrement, int points) {
        String piId = piIdOf(programIncrement);
        String teamId = teamIdOf(team);
        int number = iterationNumberOf(piId, iteration);

        // contains: genau eine Zelle für Team und Iteration, mit diesen Punkten.
        given().get(PI_PATH + "/" + piId + "/capacities")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(
                        "findAll { it.teamId == '%s' && it.iterationNumber == %d }.points".formatted(teamId, number),
                        contains(points));
    }

    private void put(String piId, String teamId, int iterationNumber, String points) {
        Response response = given().contentType(ContentType.JSON)
                .body("{\"points\": " + points + "}")
                .put(PI_PATH + "/" + piId + "/capacities/" + teamId + "/" + iterationNumber);
        context.response(response);
    }

    private String piIdOf(String name) {
        return given().get(PI_PATH)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .param("name", name)
                .getString("find { it.name == name }.id");
    }

    private String teamIdOf(String name) {
        return given().get(TEAM_PATH)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .param("name", name)
                .getString("find { it.name == name }.id");
    }

    private Optional<String> existingTeamIdOf(String name) {
        return Optional.ofNullable(teamIdOf(name));
    }

    private String createTeam(String name) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("name", name))
                .post(TEAM_PATH)
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    // Die Nummer kommt aus der Detailansicht des PI (US-5), damit das Feature mit den Namen der Oberfläche arbeitet.
    private int iterationNumberOf(String piId, String iterationName) {
        JsonPath pi = given().get(PI_PATH + "/" + piId)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();
        return pi.param("name", iterationName).getInt("iterations.find { it.name == name }.number");
    }
}
