package ch.grudligstrasse.pikel.cucumber.steps;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import ch.grudligstrasse.pikel.adapter.out.persistence.ProgramIncrementRepositoryAdapter;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Gegebensei;
import io.cucumber.java.de.Wenn;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.Map;

public class ProgramIncrementSteps {

    private static final String PATH = "/api/v1/program-increments";

    @Inject
    ProgramIncrementRepositoryAdapter repository;

    private Response response;

    // Szenarien teilen sich die Datenbank; ohne Leeren hängen Sortierung und Leerzustand von der Reihenfolge ab.
    @Gegebensei("eine leere Liste der PIs")
    public void eineLeereListe() {
        QuarkusTransaction.requiringNew().run(repository::deleteAll);
    }

    @Gegebensei("das PI {string} vom {string} bis {string}")
    public void dasPiExistiert(String name, String start, String end) {
        ichDasPiAnlege(name, start, end);
        response.then().statusCode(201);
    }

    @Wenn("ich das PI {string} vom {string} bis {string} anlege")
    public void ichDasPiAnlege(String name, String start, String end) {
        response = given().contentType(ContentType.JSON)
                .body(Map.of("name", name, "startDate", start, "endDate", end))
                .post(PATH);
    }

    @Wenn("ich das PI {string} ohne Enddatum anlege")
    public void ichDasPiOhneEnddatumAnlege(String name) {
        response = given().contentType(ContentType.JSON)
                .body(Map.of("name", name, "startDate", "2027-01-04"))
                .post(PATH);
    }

    @Wenn("ich ein PI mit dem Inhalt {string} anlege")
    public void ichEinPiMitDemInhaltAnlege(String body) {
        response = given().contentType(ContentType.JSON).body(body).post(PATH);
    }

    @Wenn("ich ein PI ohne Inhalt anlege")
    public void ichEinPiOhneInhaltAnlege() {
        response = given().contentType(ContentType.JSON).post(PATH);
    }

    @Dann("wird das PI mit einer ID angelegt")
    public void wirdDasPiAngelegt() {
        response.then().statusCode(201).body("id", notNullValue());
    }

    @Dann("erscheint {string} in der Liste der PIs")
    public void erscheintInDerListe(String name) {
        given().get(PATH).then().statusCode(200).body("name", hasItem(name));
    }

    @Dann("zeigt die Liste der PIs zuerst {string} und dann {string}")
    public void zeigtDieListeInReihenfolge(String first, String second) {
        given().get(PATH).then().statusCode(200).body("name", contains(first, second));
    }

    @Dann("wird das Anlegen mit der Meldung {string} abgelehnt")
    public void wirdAbgelehnt(String message) {
        response.then().statusCode(400).contentType(ContentType.JSON).body("message", is(message));
    }
}
