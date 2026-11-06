package ch.grudligstrasse.pikel.domain.port.out;

import ch.grudligstrasse.pikel.domain.model.ProgramIncrement;
import java.util.List;

/** Persistenz für Program Increments. */
public interface ProgramIncrementRepository {

    List<ProgramIncrement> findAllOrderedByStartDate();

    void save(ProgramIncrement programIncrement);
}
