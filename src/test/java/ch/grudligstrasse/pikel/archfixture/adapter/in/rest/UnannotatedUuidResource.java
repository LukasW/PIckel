package ch.grudligstrasse.pikel.archfixture.adapter.in.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.UUID;

/** Erlaubt: UUID-Parameter ohne Pfad-Annotation, neben einem String-Pfadparameter in derselben Klasse. */
@Path("/fixture/unannotated-uuid")
public class UnannotatedUuidResource {

    @GET
    @Path("/{id}")
    public String get(@PathParam("id") String id) {
        return describe(UUID.fromString(id));
    }

    String describe(UUID id) {
        return id.toString();
    }
}
