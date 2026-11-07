package ch.grudligstrasse.pikel.adapter.in.rest;

import ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pikel.domain.port.in.FeatureUseCase;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
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
 * Das Feature-Backlog eines PI.
 *
 * <p>Noch ohne Anmeldung: Auth (OIDC) kommt mit einer eigenen Story, danach gilt {@code @Authenticated}.
 */
@Path("/api/v1/program-increments/{piId}/features")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FeatureResource {

    private final FeatureUseCase useCase;

    public FeatureResource(FeatureUseCase useCase) {
        this.useCase = useCase;
    }

    /** Das Backlog des PI nach WSJF-Rang; 404 mit {@link ErrorResponse}, wenn es das PI nicht gibt. */
    @GET
    public List<FeatureDto> list(@PathParam("piId") String piId) {
        return useCase.listBacklog(toUuid(piId)).stream().map(FeatureDto::from).toList();
    }

    /** Erfasst ein Feature: 201 mit dem Feature; 400 bei ungültigen Angaben; 404, wenn es das PI nicht gibt. */
    @POST
    public Response add(@PathParam("piId") String piId, @NotNull FeatureRequest request) {
        FeatureDto created = FeatureDto.from(useCase.add(
                toUuid(piId),
                request.title(),
                request.description(),
                request.wsjfRank(),
                request.owner(),
                request.estimate()));
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    // Wie bei den PIs: eine ungültige UUID ist für den Aufrufer ein unbekanntes PI.
    private static UUID toUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ProgramIncrementNotFoundException();
        }
    }
}
