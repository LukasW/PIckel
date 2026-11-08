package ch.grudligstrasse.pickel.archfixture.adapter.in.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.UUID;
import org.jboss.resteasy.reactive.RestPath;

/** Verstoss: UUID als {@code @RestPath}. */
@Path("/fixture/uuid-rest-path")
public class UuidRestPathResource {

    @GET
    @Path("/{id}")
    public String get(@RestPath UUID id) {
        return id.toString();
    }
}
