package ch.grudligstrasse.pikel.cucumber.steps;

import static org.hamcrest.Matchers.is;

import io.cucumber.java.Before;
import io.cucumber.java.de.Dann;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Zustand, den mehrere Step-Klassen im selben Szenario teilen: die letzte Antwort eines Erfassens (Teams und
 * Features nutzen denselben Satz «wird das Erfassen … abgelehnt») und die IDs der PIs, die das Szenario angelegt hat.
 */
@Singleton
public class ScenarioContext {

    private Response response;

    // Name → ID; die Namen der PIs sind nicht eindeutig, darum gilt das zuletzt im Szenario angelegte PI.
    private final Map<String, String> programIncrementIds = new HashMap<>();

    @Before
    public void reset() {
        response = null;
        programIncrementIds.clear();
    }

    void response(Response response) {
        this.response = response;
    }

    void programIncrementCreated(String name, String id) {
        programIncrementIds.put(name, id);
    }

    String programIncrementId(String name) {
        String id = programIncrementIds.get(name);
        if (id == null) {
            throw new IllegalStateException("Das Szenario hat kein PI \"" + name + "\" angelegt.");
        }
        return id;
    }

    @Dann("wird das Erfassen mit der Meldung {string} abgelehnt")
    public void wirdDasErfassenAbgelehnt(String message) {
        if (response == null) {
            throw new IllegalStateException("Das Szenario hat noch nichts erfasst.");
        }
        response.then().statusCode(400).contentType(ContentType.JSON).body("message", is(message));
    }
}
