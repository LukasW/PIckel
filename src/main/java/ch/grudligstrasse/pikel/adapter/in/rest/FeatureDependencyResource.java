package ch.grudligstrasse.pikel.adapter.in.rest;

import ch.grudligstrasse.pikel.domain.model.FeatureNotFoundException;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pikel.domain.port.in.FeatureDependencyUseCase;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

/**
 * Abhängigkeiten zwischen den Features eines PI.
 *
 * <p>Noch ohne Anmeldung: Auth (OIDC) kommt mit einer eigenen Story, danach gilt {@code @Authenticated}.
 */
@Path("/api/v1/program-increments/{piId}/dependencies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FeatureDependencyResource {

    private final FeatureDependencyUseCase useCase;

    public FeatureDependencyResource(FeatureDependencyUseCase useCase) {
        this.useCase = useCase;
    }

    /** Alle Abhängigkeiten des PI; 404, wenn es das PI nicht gibt. */
    @GET
    public List<FeatureDependencyDto> list(@PathParam("piId") String piId) {
        return useCase.list(toPiUuid(piId)).stream()
                .map(FeatureDependencyDto::from)
                .toList();
    }

    /** Erfasst eine Abhängigkeit: 201; 400 bei verletzter Regel mit Meldung; 404 bei unbekanntem PI oder Feature. */
    @POST
    public Response add(@PathParam("piId") String piId, @NotNull FeatureDependencyRequest request) {
        UUID pi = toPiUuid(piId);
        // Eine fehlende ID ist für den Aufrufer ein unbekanntes Feature, wie eine ungültige UUID in der Adresse.
        if (request.needingFeatureId() == null || request.neededFeatureId() == null) {
            throw new FeatureNotFoundException();
        }
        FeatureDependencyDto created =
                FeatureDependencyDto.from(useCase.add(pi, request.needingFeatureId(), request.neededFeatureId()));
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    /** Entfernt eine Abhängigkeit: 204, auch wenn es sie nicht gibt; 404 bei unbekanntem PI. */
    @DELETE
    @Path("/{needingId}/{neededId}")
    public Response remove(
            @PathParam("piId") String piId,
            @PathParam("needingId") String needingId,
            @PathParam("neededId") String neededId) {
        useCase.remove(toPiUuid(piId), toFeatureUuid(needingId), toFeatureUuid(neededId));
        return Response.noContent().build();
    }

    private static UUID toFeatureUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new FeatureNotFoundException();
        }
    }

    private static UUID toPiUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ProgramIncrementNotFoundException();
        }
    }
}
