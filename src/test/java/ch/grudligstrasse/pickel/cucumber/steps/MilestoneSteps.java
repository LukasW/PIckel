package ch.grudligstrasse.pickel.cucumber.steps;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Gegebensei;
import io.cucumber.java.de.Wenn;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * US-15: Milestones über {@code /api/v1/program-increments/{piId}/milestones}, ihre Position über den Board-Endpunkt
 * {@code GET /api/v1/program-increments/{piId}/board}.
 */
public class MilestoneSteps {

    private static final String PI_PATH = "/api/v1/program-increments";

    // «wird das Erfassen … abgelehnt» (ScenarioContext) prüft die letzte Antwort im gemeinsamen Kontext.
    @Inject
    ScenarioContext context;

    // Name → ID, PI-ID und zuletzt gesendetes Datum der Milestones, die das Szenario angelegt oder geändert hat.
    private final Map<String, String> ids = new HashMap<>();
    private final Map<String, String> piIds = new HashMap<>();
    private final Map<String, String> dates = new HashMap<>();

    private Response response;

    @Wenn("ich im PI {string} den Milestone {string} am {string} erfasse")
    public void ichDenMilestoneErfasse(String pi, String name, String date) {
        String piId = context.programIncrementId(pi);
        response = given().contentType(ContentType.JSON)
                .body(Map.of("name", name, "date", date))
                .post(milestonesPath(piId));
        context.response(response);
        if (response.statusCode() == 201) {
            remember(name, response.path("id"), piId, date);
        }
    }

    @Gegebensei("im PI {string} der Milestone {string} am {string}")
    public void imPiDerMilestone(String pi, String name, String date) {
        ichDenMilestoneErfasse(pi, name, date);
        response.then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("name", is(name))
                .body("date", is(date));
    }

    // Der Name bleibt, nur das Datum ändert sich; PUT schickt beide Angaben.
    @Wenn("ich beim Milestone {string} das Datum auf {string} setze")
    public void ichDasDatumSetze(String name, String date) {
        response = given().contentType(ContentType.JSON)
                .body(Map.of("name", name, "date", date))
                .put(milestonesPath(piIdOf(name)) + "/" + idOf(name));
        context.response(response);
        if (response.statusCode() == 200) {
            dates.put(name, date);
        }
    }

    @Wenn("ich den Milestone {string} lösche")
    public void ichDenMilestoneLoesche(String name) {
        response = given().delete(milestonesPath(piIdOf(name)) + "/" + idOf(name));
        context.response(response);
    }

    // Fehlervertrag (ADR 0003): Die Antwort prüft «antwortet der Server mit …» aus FeatureSteps.
    @Wenn("ich im PI {string} einen unbekannten Milestone lösche")
    public void ichEinenUnbekanntenMilestoneLoesche(String programIncrement) {
        response =
                given().delete(milestonesPath(context.programIncrementId(programIncrement)) + "/" + UUID.randomUUID());
        context.response(response);
    }

    // Die Position kommt vom Board: iterationNumber zeigt auf die Spalte, deren Name das Szenario nennt.
    @Dann("liegt der Milestone {string} auf dem Board über {string}")
    public void liegtDerMilestoneUeber(String name, String iteration) {
        // 201 nach dem Erfassen, 200 nach dem Verschieben.
        response.then().statusCode(anyOf(is(201), is(200)));
        JsonPath board = board(piIdOf(name)).param("name", name);

        assertEquals(1, board.getList("milestones.findAll { it.name == name }").size(), "genau ein Marker " + name);
        assertEquals(idOf(name), board.getString("milestones.find { it.name == name }.id"));
        assertEquals(dates.get(name), board.getString("milestones.find { it.name == name }.date"));
        int number = board.getInt("milestones.find { it.name == name }.iterationNumber");
        assertEquals(iteration, board.getString("iterations[" + (number - 1) + "].name"));
    }

    @Dann("hat das Board von {string} keine Milestones")
    public void hatDasBoardKeineMilestones(String pi) {
        response.then().statusCode(204);
        String piId = context.programIncrementId(pi);
        given().get(PI_PATH + "/" + piId + "/board")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("milestones", hasSize(0));
        given().get(milestonesPath(piId)).then().statusCode(200).body("$", hasSize(0));
    }

    private void remember(String name, String id, String piId, String date) {
        ids.put(name, id);
        piIds.put(name, piId);
        dates.put(name, date);
    }

    private String idOf(String name) {
        return known(ids, name);
    }

    private String piIdOf(String name) {
        return known(piIds, name);
    }

    private static String known(Map<String, String> values, String name) {
        String value = values.get(name);
        if (value == null) {
            throw new IllegalStateException("Das Szenario hat keinen Milestone \"" + name + "\" angelegt.");
        }
        return value;
    }

    private static JsonPath board(String piId) {
        return given().get(PI_PATH + "/" + piId + "/board")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath();
    }

    private static String milestonesPath(String piId) {
        return PI_PATH + "/" + piId + "/milestones";
    }
}
