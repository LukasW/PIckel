package ch.grudligstrasse.pikel.adapter.in.rest;

import ch.grudligstrasse.pikel.domain.model.FeatureNotFoundException;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pikel.domain.port.in.FeatureUseCase;
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
 * Das Feature-Backlog eines PI.
 *
 * <p>Noch ohne Anmeldung: Auth (OIDC) kommt mit einer eigenen Story, danach gilt {@code @Authenticated}.
 */
@Path("/v1/program-increments/{piId}/features")
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

    /** Ändert ein Feature: 200 mit dem Feature; 400 wie beim Erfassen; 404 bei unbekanntem PI oder Feature. */
    @PUT
    @Path("/{id}")
    public FeatureDto update(
            @PathParam("piId") String piId, @PathParam("id") String id, @NotNull FeatureRequest request) {
        return FeatureDto.from(useCase.update(
                toUuid(piId),
                toFeatureUuid(id),
                request.title(),
                request.description(),
                request.wsjfRank(),
                request.owner(),
                request.estimate()));
    }

    /**
     * Plant ein Feature in die Zelle Team × Iteration ein: 200 mit dem Feature; 400 bei der IP-Iteration, einer
     * Iteration, die es im PI nicht gibt, oder fehlenden Angaben; 404 bei unbekanntem PI, Feature oder Team.
     */
    @PUT
    @Path("/{id}/scheduling")
    public FeatureDto schedule(
            @PathParam("piId") String piId, @PathParam("id") String id, @NotNull SchedulingRequest request) {
        return FeatureDto.from(
                useCase.schedule(toUuid(piId), toFeatureUuid(id), request.teamId(), request.iterationNumber()));
    }

    /** Nimmt ein Feature zurück ins Backlog: 200 mit dem Feature; 404 bei unbekanntem PI oder Feature. */
    @DELETE
    @Path("/{id}/scheduling")
    public FeatureDto unschedule(@PathParam("piId") String piId, @PathParam("id") String id) {
        return FeatureDto.from(useCase.unschedule(toUuid(piId), toFeatureUuid(id)));
    }

    /** Löscht ein Feature: 204; 404 bei unbekanntem PI oder Feature. */
    @DELETE
    @Path("/{id}")
    public Response remove(@PathParam("piId") String piId, @PathParam("id") String id) {
        useCase.remove(toUuid(piId), toFeatureUuid(id));
        return Response.noContent().build();
    }

    /**
     * Importiert eine CSV-Datei (UTF-8) ins Backlog: 201 mit der Anzahl; 400 mit Zeile und Grund, dann wurde nichts
     * importiert; 404, wenn es das PI nicht gibt.
     */
    @POST
    @Path("/import")
    @Consumes("text/csv")
    public Response importCsv(@PathParam("piId") String piId, @NotNull String csv) {
        int imported = useCase.importCsv(toUuid(piId), csv).size();
        return Response.status(Response.Status.CREATED)
                .entity(new FeatureImportDto(imported))
                .build();
    }

    private static UUID toFeatureUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new FeatureNotFoundException();
        }
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
