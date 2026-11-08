package ch.grudligstrasse.pickel.adapter.in.rest;

import ch.grudligstrasse.pickel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pickel.domain.port.in.BoardUseCase;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;

/**
 * Das Program Board eines PI (nur lesend).
 *
 * <p>Noch ohne Anmeldung: Auth (OIDC) kommt mit einer eigenen Story, danach gilt {@code @Authenticated}.
 */
@Path("/v1/program-increments/{piId}/board")
@Produces(MediaType.APPLICATION_JSON)
public class BoardResource {

    private final BoardUseCase useCase;

    public BoardResource(BoardUseCase useCase) {
        this.useCase = useCase;
    }

    /** Das Board des PI; 404 mit {@link ErrorResponse}, wenn es das PI nicht gibt. */
    @GET
    public BoardDto get(@PathParam("piId") String piId) {
        return BoardDto.from(useCase.get(toUuid(piId)));
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
