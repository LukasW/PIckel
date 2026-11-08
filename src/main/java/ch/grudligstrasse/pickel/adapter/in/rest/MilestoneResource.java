package ch.grudligstrasse.pickel.adapter.in.rest;

import ch.grudligstrasse.pickel.domain.model.Milestone;
import ch.grudligstrasse.pickel.domain.model.MilestoneNotFoundException;
import ch.grudligstrasse.pickel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pickel.domain.port.in.MilestoneUseCase;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Milestones eines PI.
 *
 * <p>Noch ohne Anmeldung: Auth (OIDC) kommt mit einer eigenen Story, danach gilt {@code @Authenticated}.
 */
@Path("/v1/program-increments/{id}/milestones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MilestoneResource {

    private final MilestoneUseCase useCase;

    public MilestoneResource(MilestoneUseCase useCase) {
        this.useCase = useCase;
    }

    /** Die Milestones des PI, aufsteigend nach Datum; 404 ohne PI. */
    @GET
    public List<MilestoneDto> list(@PathParam("id") String id) {
        return useCase.list(piId(id)).stream().map(MilestoneDto::from).toList();
    }

    /** Erfasst einen Milestone: 201 mit dem Milestone; 400 bei ungültigen Angaben; 404 ohne PI. */
    @POST
    public Response add(@PathParam("id") String id, @NotNull MilestoneRequest request, @Context UriInfo uriInfo) {
        Milestone created = useCase.add(piId(id), request.name(), request.date());
        URI location =
                uriInfo.getAbsolutePathBuilder().path(created.id().toString()).build();
        return Response.created(location).entity(MilestoneDto.from(created)).build();
    }

    /** Ändert Name und Datum: 200 mit dem Milestone; 400 bei ungültigen Angaben; 404 ohne PI oder Milestone. */
    @PUT
    @Path("/{milestoneId}")
    public MilestoneDto update(
            @PathParam("id") String id,
            @PathParam("milestoneId") String milestoneId,
            @NotNull MilestoneRequest request) {
        UUID programIncrementId = piId(id);
        return MilestoneDto.from(
                useCase.update(programIncrementId, milestoneId(milestoneId), request.name(), request.date()));
    }

    /** Löscht den Milestone: 204; 404 ohne PI oder Milestone. */
    @DELETE
    @Path("/{milestoneId}")
    public Response delete(@PathParam("id") String id, @PathParam("milestoneId") String milestoneId) {
        UUID programIncrementId = piId(id);
        useCase.delete(programIncrementId, milestoneId(milestoneId));
        return Response.noContent().build();
    }

    // Wie bei PIs: eine ungültige UUID ist für den Aufrufer ein unbekanntes PI, kein Quarkus-404 ohne Meldung.
    private static UUID piId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ProgramIncrementNotFoundException();
        }
    }

    private static UUID milestoneId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new MilestoneNotFoundException();
        }
    }
}
