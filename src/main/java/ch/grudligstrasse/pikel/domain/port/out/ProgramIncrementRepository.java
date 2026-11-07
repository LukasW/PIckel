package ch.grudligstrasse.pikel.domain.port.out;

import ch.grudligstrasse.pikel.domain.model.ProgramIncrement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistenz für Program Increments. */
public interface ProgramIncrementRepository {

    List<ProgramIncrement> findAllOrderedByStartDate();

    Optional<ProgramIncrement> findOne(UUID id);

    void save(ProgramIncrement programIncrement);
}
