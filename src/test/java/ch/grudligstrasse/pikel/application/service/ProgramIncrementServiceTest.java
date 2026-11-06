package ch.grudligstrasse.pikel.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ch.grudligstrasse.pikel.domain.model.InvalidProgramIncrementException;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrement;
import ch.grudligstrasse.pikel.domain.port.out.ProgramIncrementRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgramIncrementServiceTest {

    @Mock
    ProgramIncrementRepository repository;

    @InjectMocks
    ProgramIncrementService service;

    @Test
    void planSavesAndReturnsNewProgramIncrement() {
        ProgramIncrement created =
                service.plan("PI 27.1", LocalDate.of(2027, 1, 4), LocalDate.of(2027, 3, 26));

        verify(repository).save(created);
        assertEquals("PI 27.1", created.name());
    }

    @Test
    void planDoesNotSaveInvalidProgramIncrement() {
        LocalDate day = LocalDate.of(2027, 1, 4);

        assertThrows(InvalidProgramIncrementException.class, () -> service.plan("PI 27.1", day, day));
        verify(repository, never()).save(any());
    }
}
