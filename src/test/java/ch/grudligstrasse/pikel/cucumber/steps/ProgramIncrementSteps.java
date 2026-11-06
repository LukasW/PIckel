package ch.grudligstrasse.pikel.cucumber.steps;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;

public class ProgramIncrementSteps {

    private static final String PATH = "/api/v1/program-increments";

    private Response response;

    @Wenn("ich das PI {string} vom {string} bis {string} anlege")
    public void ichDasPiAnlege(String name, String start, String end) {
        response = given().contentType(ContentType.JSON)
                .body(Map.of("name", name, "startDate", start, "endDate", end))
                .post(PATH);
    }

    @Dann("wird das PI mit einer ID angelegt")
    public void wirdDasPiAngelegt() {
        response.then().statusCode(201).body("id", notNullValue());
    }

    @Dann("erscheint {string} in der Liste der PIs")
    public void erscheintInDerListe(String name) {
        given().get(PATH).then().statusCode(200).body("name", hasItem(name));
    }

    @Dann("wird das Anlegen mit der Meldung {string} abgelehnt")
    public void wirdAbgelehnt(String message) {
        response.then().statusCode(400).body("message", is(message));
    }
}
