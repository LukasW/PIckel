package ch.grudligstrasse.pikel.adapter.in.rest;

import ch.grudligstrasse.pikel.domain.model.ProgramIncrement;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pikel.domain.port.in.ProgramIncrementUseCase;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
 * Program Increments.
 *
 * <p>Noch ohne Anmeldung: Auth (OIDC) kommt mit einer eigenen Story, danach gilt {@code @Authenticated}.
 */
@Path("/api/v1/program-increments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProgramIncrementResource {

    private final ProgramIncrementUseCase useCase;

    public ProgramIncrementResource(ProgramIncrementUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * Ein PI mit seinen Iterationen; 404 mit {@link ErrorResponse}, wenn es die ID nicht gibt. Die ID kommt als Text,
     * weil Quarkus eine ungültige UUID sonst mit einem 404 ohne Meldung beantwortet; für den Aufrufer ist auch sie ein
     * unbekanntes PI.
     */
    @GET
    @Path("/{id}")
    public ProgramIncrementDto get(@PathParam("id") String id) {
        return ProgramIncrementDto.from(useCase.get(toUuid(id)));
    }

    private static UUID toUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ProgramIncrementNotFoundException();
        }
    }

    /** Alle PIs, aufsteigend nach Startdatum. */
    @GET
    public List<ProgramIncrementDto> list() {
        return useCase.listAll().stream().map(ProgramIncrementDto::from).toList();
    }

    /**
     * Legt ein PI an. 201 mit dem neuen PI und {@code Location} auf {@link #get}; 400 mit {@link ErrorResponse} bei
     * fehlenden oder ungültigen Angaben. Ohne Iterationslänge gelten 2 Wochen.
     */
    @POST
    public Response plan(@NotNull PlanProgramIncrementRequest request, @Context UriInfo uriInfo) {
        ProgramIncrement created =
                useCase.plan(request.name(), request.startDate(), request.endDate(), request.iterationLengthWeeks());
        URI location =
                uriInfo.getAbsolutePathBuilder().path(created.id().toString()).build();
        return Response.created(location)
                .entity(ProgramIncrementDto.from(created))
                .build();
    }
}
