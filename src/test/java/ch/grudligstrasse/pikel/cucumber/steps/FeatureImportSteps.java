package ch.grudligstrasse.pikel.cucumber.steps;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** US-11: CSV-Import ins Backlog. Das Backlog selbst prüfen die Steps von {@link FeatureSteps}. */
public class FeatureImportSteps {

    private static final String PI_PATH = "/api/v1/program-increments";
    private static final String TEXT_CSV = "text/csv; charset=UTF-8";

    @Inject
    ScenarioContext context;

    private Response response;

    @Wenn("ich ins PI {string} diese CSV importiere:")
    public void ichDieseCsvImportiere(String pi, String csv) {
        importiere(context.programIncrementId(pi), csv);
    }

    @Wenn("ich in ein unbekanntes PI diese CSV importiere:")
    public void ichInEinUnbekanntesPiImportiere(String csv) {
        importiere(UUID.randomUUID().toString(), csv);
    }

    @Wenn("ich ins PI {string} eine CSV mit {int} Datenzeilen importiere")
    public void ichEineCsvMitDatenzeilenImportiere(String pi, int rows) {
        StringBuilder csv = new StringBuilder("Titel\n");
        for (int row = 1; row <= rows; row++) {
            csv.append("Feature ").append(row).append('\n');
        }
        importiere(context.programIncrementId(pi), csv.toString());
    }

    // Gültige Zeilen (unter 1000, Beschreibung unter 5000 Zeichen), damit nur die Grösse ablehnen kann.
    @Wenn("ich ins PI {string} eine CSV mit {int} Bytes importiere")
    public void ichEineCsvMitBytesImportiere(String pi, int bytes) {
        String header = "Titel;Beschreibung\n";
        String description = "x".repeat(2000);
        StringBuilder csv = new StringBuilder(header);
        int row = 0;
        while (csv.length() + 4000 < bytes) {
            csv.append("F").append(++row).append(';').append(description).append('\n');
        }
        String last = "Rest;";
        int rest = bytes - csv.length() - last.length();
        csv.append(last).append("x".repeat(rest));
        if (csv.toString().getBytes(StandardCharsets.UTF_8).length != bytes) {
            throw new IllegalStateException("Die CSV hat nicht " + bytes + " Bytes.");
        }
        importiere(context.programIncrementId(pi), csv.toString());
    }

    @Dann("wurde(n) {int} Feature(s) importiert")
    public void wurdenFeaturesImportiert(int count) {
        response.then().statusCode(201).contentType(ContentType.JSON).body("imported", is(count));
    }

    @Dann("wird der Import mit der Meldung {string} abgelehnt")
    public void wirdDerImportAbgelehnt(String message) {
        wirdDerImportMitDemStatusAbgelehnt(400, message);
    }

    @Dann("wird der Import mit dem Status {int} und der Meldung {string} abgelehnt")
    public void wirdDerImportMitDemStatusAbgelehnt(int status, String message) {
        response.then().statusCode(status).contentType(ContentType.JSON).body("message", is(message));
    }

    private void importiere(String piId, String csv) {
        response = given().contentType(TEXT_CSV)
                .body(csv.getBytes(StandardCharsets.UTF_8))
                .post(PI_PATH + "/" + piId + "/features/import");
        context.response(response);
    }
}
