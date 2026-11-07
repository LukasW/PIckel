package ch.grudligstrasse.pikel.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.grudligstrasse.pikel.domain.model.InvalidProgramIncrementException;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrement;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pikel.domain.port.out.ProgramIncrementRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgramIncrementServiceTest {

    private static final LocalDate START = LocalDate.of(2027, 1, 4);
    private static final LocalDate END = LocalDate.of(2027, 3, 26);

    @Mock
    ProgramIncrementRepository repository;

    @InjectMocks
    ProgramIncrementService service;

    @Test
    void listAllReturnsProgramIncrementsInRepositoryOrder() {
        ProgramIncrement first = new ProgramIncrement(UUID.randomUUID(), "PI 27.1", START, END, 2);
        ProgramIncrement second = new ProgramIncrement(
                UUID.randomUUID(), "PI 27.2", LocalDate.of(2027, 4, 5), LocalDate.of(2027, 6, 25), 2);
        when(repository.findAllOrderedByStartDate()).thenReturn(List.of(first, second));

        assertEquals(List.of(first, second), service.listAll());
    }

    @Test
    void planSavesAndReturnsNewProgramIncrement() {
        ProgramIncrement created = service.plan("PI 27.1", START, END, null);

        verify(repository).save(created);
        assertEquals("PI 27.1", created.name());
    }

    @Test
    void planDoesNotSaveInvalidProgramIncrement() {
        assertThrows(InvalidProgramIncrementException.class, () -> service.plan("PI 27.1", START, START, null));
        verify(repository, never()).save(any());
    }

    @Test
    void planPassesChosenIterationLength() {
        ProgramIncrement created = service.plan("PI 27.1", START, END, 3);

        verify(repository).save(created);
        assertEquals(3, created.iterationLengthWeeks());
    }

    @Test
    void planWithoutIterationLengthUsesTwoWeeks() {
        ProgramIncrement created = service.plan("PI 27.1", START, END, null);

        assertEquals(2, created.iterationLengthWeeks());
    }

    @Test
    void planDoesNotSaveProgramIncrementWithInvalidIterationLength() {
        assertThrows(InvalidProgramIncrementException.class, () -> service.plan("PI 27.1", START, END, 5));
        verify(repository, never()).save(any());
    }

    @Test
    void getReturnsProgramIncrementOfId() {
        ProgramIncrement stored = new ProgramIncrement(UUID.randomUUID(), "PI 27.1", START, END, 3);
        when(repository.findOne(stored.id())).thenReturn(Optional.of(stored));

        assertEquals(stored, service.get(stored.id()));
    }

    @Test
    void getRejectsUnknownId() {
        UUID unknown = UUID.randomUUID();
        when(repository.findOne(unknown)).thenReturn(Optional.empty());

        assertThrows(ProgramIncrementNotFoundException.class, () -> service.get(unknown));
    }
}
