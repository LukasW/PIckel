package ch.grudligstrasse.pikel.adapter.in.rest;

import ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pikel.domain.model.TeamNotFoundException;
import ch.grudligstrasse.pikel.domain.port.in.CapacityUseCase;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

/**
 * Kapazität der Teams je Iteration eines PI.
 *
 * <p>Noch ohne Anmeldung: Auth (OIDC) kommt mit einer eigenen Story, danach gilt {@code @Authenticated}.
 */
@Path("/v1/program-increments/{id}/capacities")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CapacityResource {

    private final CapacityUseCase useCase;

    public CapacityResource(CapacityUseCase useCase) {
        this.useCase = useCase;
    }

    /** Jede Zelle Team × Entwicklungs-Iteration, ohne Eintrag mit 0; 404 ohne PI. */
    @GET
    public List<CapacityDto> list(@PathParam("id") String id) {
        return useCase.list(piId(id)).stream().map(CapacityDto::from).toList();
    }

    /** Erfasst die Kapazität: 200 mit der Zelle; 400 bei ungültigen Punkten oder Iteration; 404 ohne PI oder Team. */
    @PUT
    @Path("/{teamId}/{iterationNumber}")
    public CapacityDto set(
            @PathParam("id") String id,
            @PathParam("teamId") String teamId,
            @PathParam("iterationNumber") int iterationNumber,
            @NotNull CapacityRequest request) {
        UUID programIncrementId = piId(id);
        UUID team;
        try {
            team = UUID.fromString(teamId);
        } catch (IllegalArgumentException e) {
            throw new TeamNotFoundException();
        }
        return CapacityDto.from(useCase.set(programIncrementId, team, iterationNumber, request.points()));
    }

    // Wie bei PIs: eine ungültige UUID ist für den Aufrufer ein unbekanntes PI, kein Quarkus-404 ohne Meldung.
    private static UUID piId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ProgramIncrementNotFoundException();
        }
    }
}
