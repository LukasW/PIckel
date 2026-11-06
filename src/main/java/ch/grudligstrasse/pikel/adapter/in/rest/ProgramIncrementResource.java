package ch.grudligstrasse.pikel.adapter.in.rest;

import ch.grudligstrasse.pikel.domain.model.ProgramIncrement;
import ch.grudligstrasse.pikel.domain.port.in.ProgramIncrementUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

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

    /** Alle PIs, aufsteigend nach Startdatum. */
    @GET
    public List<ProgramIncrementDto> list() {
        return useCase.listAll().stream().map(ProgramIncrementDto::from).toList();
    }

    /** Legt ein PI an. 201 mit dem neuen PI; 400 bei fehlenden oder ungültigen Angaben. */
    @POST
    public Response plan(@Valid @NotNull PlanProgramIncrementRequest request) {
        ProgramIncrement created = useCase.plan(request.name(), request.startDate(), request.endDate());
        return Response.created(URI.create("/api/v1/program-increments/" + created.id()))
                .entity(ProgramIncrementDto.from(created))
                .build();
    }
}
