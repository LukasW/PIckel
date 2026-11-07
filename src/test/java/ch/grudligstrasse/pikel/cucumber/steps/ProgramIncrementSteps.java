package ch.grudligstrasse.pikel.cucumber.steps;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.withArgs;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ProgramIncrementSteps {

    private static final String PATH = "/api/v1/program-increments";

    @Inject
    ProgramIncrementRepositoryAdapter repository;

    private Response response;
    private String addedLinkUrl;

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

    @Wenn("ich das PI {string} vom {string} bis {string} mit Iterationen zu {int} Wochen anlege")
    public void ichDasPiMitIterationslaengeAnlege(String name, String start, String end, int weeks) {
        response = given().contentType(ContentType.JSON)
                .body(Map.of("name", name, "startDate", start, "endDate", end, "iterationLengthWeeks", weeks))
                .post(PATH);
    }

    // Derselbe Request wie in US-1: Das Feld fehlt ganz, es wird nicht als null geschickt.
    @Wenn("ich das PI {string} vom {string} bis {string} ohne Iterationslänge anlege")
    public void ichDasPiOhneIterationslaengeAnlege(String name, String start, String end) {
        ichDasPiAnlege(name, start, end);
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

    @Wenn("ich ein PI mit einer unbekannten ID abfrage")
    public void ichEinPiMitUnbekannterIdAbfrage() {
        response = given().get(PATH + "/" + UUID.randomUUID());
    }

    @Wenn("ich das PI mit der ID {string} abfrage")
    public void ichDasPiMitDerIdAbfrage(String id) {
        response = given().get(PATH + "/" + id);
    }

    @Dann("wird das PI mit einer ID angelegt")
    public void wirdDasPiAngelegt() {
        response.then().statusCode(201).body("id", notNullValue());
    }

    @Dann("verweist die Antwort im Header `Location` auf das neue PI")
    public void verweistLocationAufDasNeuePi() {
        String id = response.then().statusCode(201).extract().path("id");
        response.then().header("Location", endsWith(PATH + "/" + id));

        given().get(response.header("Location")).then().statusCode(200).body("id", is(id));
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

    // Prüft Antwort und gespeicherten Stand: Die Iterationen von GET /{id} entstehen aus der gespeicherten Länge.
    @Dann("hat das PI {int} Iterationen")
    public void hatDasPiIterationen(int count) {
        response.then().statusCode(201).body("iterations", hasSize(count));
        storedProgramIncrement().then().body("iterations", hasSize(count));
    }

    @Dann("beginnt {string} am {string} und endet am {string}")
    public void beginntUndEndet(String iteration, String start, String end) {
        storedProgramIncrement()
                .then()
                .body("iterations.find { it.name == '%s' }.startDate", withArgs(iteration), is(start))
                .body("iterations.find { it.name == '%s' }.endDate", withArgs(iteration), is(end));
    }

    @Dann("wird die Abfrage mit dem Status {int} und der Meldung {string} abgelehnt")
    public void wirdDieAbfrageAbgelehnt(int status, String message) {
        response.then().statusCode(status).contentType(ContentType.JSON).body("message", is(message));
    }

    // US-8

    @Wenn("ich für {string} den Business-Kontext {string} erfasse")
    public void ichDenBusinessKontextErfasse(String name, String businessContext) {
        response = given().contentType(ContentType.JSON)
                .body(Map.of("businessContext", businessContext, "artObjectives", ""))
                .put(PATH + "/" + idOf(name) + "/business-context");
    }

    @Wenn("ich für {string} den Link {string} mit der URL {string} hinzufüge")
    public void ichDenLinkHinzufuege(String name, String title, String url) {
        addedLinkUrl = url;
        response = given().contentType(ContentType.JSON)
                .body(Map.of("title", title, "url", url))
                .post(PATH + "/" + idOf(name) + "/links");
    }

    // Prüft Antwort und gespeicherten Stand: Das PUT liefert das ganze PI, GET /{id} zeigt, dass es gespeichert ist.
    @Dann("zeigt {string} den Business-Kontext {string}")
    public void zeigtDenBusinessKontext(String name, String businessContext) {
        response.then().statusCode(200).body("name", is(name)).body("businessContext", is(businessContext));
        given().get(PATH + "/" + idOf(name))
                .then()
                .statusCode(200)
                .body("businessContext", is(businessContext))
                .body("artObjectives", is(""));
    }

    @Dann("zeigt {string} den Link {string}")
    public void zeigtDenLink(String name, String title) {
        response.then().statusCode(200).body("links.title", contains(title));
        given().get(PATH + "/" + idOf(name))
                .then()
                .statusCode(200)
                .body("links.title", contains(title))
                .body("links[0].id", notNullValue())
                .body("links[0].url", is(addedLinkUrl));
    }

    // Prüft auch den gespeicherten Stand: Ein abgelehnter Link darf nicht doch am PI hängen.
    @Dann("wird das Hinzufügen mit der Meldung {string} abgelehnt")
    public void wirdDasHinzufuegenAbgelehnt(String message) {
        response.then().statusCode(400).contentType(ContentType.JSON).body("message", is(message));
        given().get(PATH + "/" + onlyProgramIncrementId())
                .then()
                .statusCode(200)
                .body("links", hasSize(0));
    }

    @Gegebensei("für {string} ist der Business-Kontext {string} erfasst")
    public void istDerBusinessKontextErfasst(String name, String businessContext) {
        ichDenBusinessKontextErfasse(name, businessContext);
        response.then().statusCode(200);
    }

    @Gegebensei("für {string} sind die Links {string} und {string} erfasst")
    public void sindDieLinksErfasst(String name, String firstTitle, String secondTitle) {
        for (String title : new String[] {firstTitle, secondTitle}) {
            ichDenLinkHinzufuege(name, title, "https://wiki.example.ch/" + title.toLowerCase(Locale.ROOT));
            response.then().statusCode(200);
        }
    }

    @Wenn("ich für {string} den Link {string} entferne")
    public void ichDenLinkEntferne(String name, String title) {
        String id = idOf(name);
        String linkId = given().get(PATH + "/" + id)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .param("title", title)
                .getString("links.find { it.title == title }.id");
        response = given().delete(PATH + "/" + id + "/links/" + linkId);
    }

    // Prüft den gespeicherten Stand: Der entfernte Link darf in der Datenbank nicht übrig bleiben.
    @Dann("zeigt {string} nur noch den Link {string}")
    public void zeigtNurNochDenLink(String name, String title) {
        response.then().statusCode(200).body("links.title", contains(title));
        given().get(PATH + "/" + idOf(name)).then().statusCode(200).body("links.title", contains(title));
    }

    private String idOf(String name) {
        return given().get(PATH)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .param("name", name)
                .getString("find { it.name == name }.id");
    }

    private String onlyProgramIncrementId() {
        return given().get(PATH)
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .extract()
                .path("[0].id");
    }

    private Response storedProgramIncrement() {
        String id = response.then().statusCode(201).extract().path("id");
        Response stored = given().get(PATH + "/" + id);
        stored.then().statusCode(200);
        return stored;
    }
}
