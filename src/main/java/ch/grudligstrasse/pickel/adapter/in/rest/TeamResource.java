package ch.grudligstrasse.pickel.adapter.in.rest;

import ch.grudligstrasse.pickel.domain.model.TeamNotFoundException;
import ch.grudligstrasse.pickel.domain.port.in.TeamUseCase;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

/**
 * Teams des ART.
 *
 * <p>Noch ohne Anmeldung: Auth (OIDC) kommt mit einer eigenen Story, danach gilt {@code @Authenticated}.
 */
@Path("/v1/teams")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TeamResource {

    private final TeamUseCase useCase;

    public TeamResource(TeamUseCase useCase) {
        this.useCase = useCase;
    }

    /** Alle Teams, alphabetisch nach Name. */
    @GET
    public List<TeamDto> list() {
        return useCase.listAll().stream().map(TeamDto::from).toList();
    }

    /** Erfasst ein Team: 201 mit dem Team; 400 mit {@link ErrorResponse} bei ungültigem oder doppeltem Namen. */
    @POST
    public Response add(@NotNull TeamRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(TeamDto.from(useCase.add(request.name())))
                .build();
    }

    /** Benennt um: 200 mit dem Team; 400 wie beim Erfassen; 404, wenn es die ID nicht gibt. */
    @PUT
    @Path("/{id}")
    public TeamDto rename(@PathParam("id") String id, @NotNull TeamRequest request) {
        return TeamDto.from(useCase.rename(toUuid(id), request.name()));
    }

    /** Entfernt: 204; 404, wenn es die ID nicht gibt. */
    @DELETE
    @Path("/{id}")
    public Response remove(@PathParam("id") String id) {
        useCase.remove(toUuid(id));
        return Response.noContent().build();
    }

    // Wie bei PIs: eine ungültige UUID ist für den Aufrufer ein unbekanntes Team, kein Quarkus-404 ohne Meldung.
    private static UUID toUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new TeamNotFoundException();
        }
    }
}
