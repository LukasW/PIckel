package ch.grudligstrasse.pikel.cucumber.steps;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import ch.grudligstrasse.pikel.adapter.out.persistence.TeamRepositoryAdapter;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Gegebensei;
import io.cucumber.java.de.Wenn;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class TeamSteps {

    private static final String PATH = "/api/v1/teams";

    @Inject
    TeamRepositoryAdapter repository;

    // «wird das Erfassen … abgelehnt» teilen sich Teams und Features (US-9); die Antwort liegt darum dort.
    @Inject
    ScenarioContext context;

    private Response response;

    // Name → ID der Teams, die das Szenario angelegt hat; Umbenennen und Entfernen gehen über die ID.
    private final Map<String, String> ids = new HashMap<>();

    // Szenarien teilen sich die Datenbank; ohne Leeren hängen Sortierung und Duplikate von der Reihenfolge ab.
    @Gegebensei("eine leere Teamliste")
    public void eineLeereTeamliste() {
        QuarkusTransaction.requiringNew().run(repository::deleteAll);
    }

    @Gegebensei("das Team {string}")
    public void dasTeamExistiert(String name) {
        ichDasTeamErfasse(name);
        String id = response.then().statusCode(201).extract().path("id");
        ids.put(name, id);
    }

    @Wenn("ich das Team {string} erfasse")
    public void ichDasTeamErfasse(String name) {
        response =
                given().contentType(ContentType.JSON).body(Map.of("name", name)).post(PATH);
        context.response(response);
    }

    @Wenn("ich ein Team mit {int} Zeichen im Namen erfasse")
    public void ichEinTeamMitZeichenErfasse(int length) {
        ichDasTeamErfasse("x".repeat(length));
    }

    @Wenn("ich das Team {string} in {string} umbenenne")
    public void ichDasTeamUmbenenne(String name, String newName) {
        ichDasTeamMitDerIdUmbenenne(idOf(name), newName);
    }

    @Wenn("ich das Team mit der ID {string} in {string} umbenenne")
    public void ichDasTeamMitDerIdUmbenenne(String id, String newName) {
        response = given().contentType(ContentType.JSON)
                .body(Map.of("name", newName))
                .put(PATH + "/" + id);
    }

    @Wenn("ich das Team {string} entferne")
    public void ichDasTeamEntferne(String name) {
        ichDasTeamMitDerIdEntferne(idOf(name));
    }

    @Wenn("ich das Team mit der ID {string} entferne")
    public void ichDasTeamMitDerIdEntferne(String id) {
        response = given().delete(PATH + "/" + id);
    }

    @Dann("erscheint {string} in der Teamliste")
    public void erscheintInDerTeamliste(String name) {
        given().get(PATH).then().statusCode(200).body("name", hasItem(name));
    }

    @Dann("erscheint {string} nicht mehr in der Teamliste")
    public void erscheintNichtMehrInDerTeamliste(String name) {
        given().get(PATH).then().statusCode(200).body("name", not(hasItem(name)));
    }

    @Dann("zeigt die Teamliste zuerst {string} und dann {string}")
    public void zeigtDieTeamlisteInReihenfolge(String first, String second) {
        given().get(PATH).then().statusCode(200).body("name", contains(first, second));
    }

    @Dann("ist die Teamliste leer")
    public void istDieTeamlisteLeer() {
        given().get(PATH).then().statusCode(200).body("$", is(empty()));
    }

    @Dann("antwortet das Umbenennen mit dem Team {string}")
    public void antwortetDasUmbenennenMitDemTeam(String name) {
        response.then().statusCode(200).contentType(ContentType.JSON).body("name", is(name));
    }

    @Dann("antwortet das Entfernen ohne Inhalt")
    public void antwortetDasEntfernenOhneInhalt() {
        response.then().statusCode(204);
    }

    @Dann("wird die Änderung mit dem Status {int} und der Meldung {string} abgelehnt")
    public void wirdDieAenderungAbgelehnt(int status, String message) {
        response.then().statusCode(status).contentType(ContentType.JSON).body("message", is(message));
    }

    private String idOf(String name) {
        String id = ids.get(name);
        if (id == null) {
            throw new IllegalStateException("Das Szenario hat kein Team \"" + name + "\" angelegt.");
        }
        return id;
    }
}
