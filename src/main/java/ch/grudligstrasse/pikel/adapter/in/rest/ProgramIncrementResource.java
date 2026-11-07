package ch.grudligstrasse.pikel.adapter.in.rest;

import ch.grudligstrasse.pikel.domain.model.PiLinkNotFoundException;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrement;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pikel.domain.port.in.ProgramIncrementUseCase;
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

    /** Ersetzt Business-Kontext und PI Objectives des ART; 200 mit dem PI, 400 bei zu langem Text, 404 ohne PI. */
    @PUT
    @Path("/{id}/business-context")
    public ProgramIncrementDto updateBusinessContext(
            @PathParam("id") String id, @NotNull UpdateBusinessContextRequest request) {
        return ProgramIncrementDto.from(
                useCase.updateBusinessContext(toUuid(id), request.businessContext(), request.artObjectives()));
    }

    /** Fügt einen Link hinzu; 200 mit dem PI, 400 bei ungültigem Titel oder URL, 404 ohne PI. */
    @POST
    @Path("/{id}/links")
    public ProgramIncrementDto addLink(@PathParam("id") String id, @NotNull AddPiLinkRequest request) {
        return ProgramIncrementDto.from(useCase.addLink(toUuid(id), request.title(), request.url()));
    }

    /** Entfernt einen Link; 200 mit dem PI, 404 ohne PI oder ohne Link (auch bei einer ungültigen Link-ID). */
    @DELETE
    @Path("/{id}/links/{linkId}")
    public ProgramIncrementDto removeLink(@PathParam("id") String id, @PathParam("linkId") String linkId) {
        UUID programIncrementId = toUuid(id);
        UUID link;
        try {
            link = UUID.fromString(linkId);
        } catch (IllegalArgumentException e) {
            throw new PiLinkNotFoundException();
        }
        return ProgramIncrementDto.from(useCase.removeLink(programIncrementId, link));
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
