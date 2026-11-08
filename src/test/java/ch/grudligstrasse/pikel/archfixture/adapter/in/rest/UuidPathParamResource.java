package ch.grudligstrasse.pikel.archfixture.adapter.in.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.UUID;

/** Verstoss: UUID als {@code @PathParam}. Paketname endet auf {@code adapter.in.rest}, liegt aber nicht im Produktionspaket. */
@Path("/fixture/uuid-path-param")
public class UuidPathParamResource {

    @GET
    @Path("/{id}")
    public String get(@PathParam("id") UUID id) {
        return id.toString();
    }
}
