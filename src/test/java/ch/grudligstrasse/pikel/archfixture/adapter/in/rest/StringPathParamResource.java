package ch.grudligstrasse.pikel.archfixture.adapter.in.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.UUID;

/** Erlaubt: Pfadparameter als String, die Umwandlung in UUID geschieht in der Resource. */
@Path("/fixture/string-path-param")
public class StringPathParamResource {

    @GET
    @Path("/{id}")
    public String get(@PathParam("id") String id) {
        return UUID.fromString(id).toString();
    }
}
