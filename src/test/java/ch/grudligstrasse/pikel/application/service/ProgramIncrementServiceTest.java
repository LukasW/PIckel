package ch.grudligstrasse.pikel.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.grudligstrasse.pikel.domain.model.InvalidProgramIncrementException;
import ch.grudligstrasse.pikel.domain.model.PiLink;
import ch.grudligstrasse.pikel.domain.model.PiLinkNotFoundException;
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

    // US-8

    private static final String VISION_URL = "https://wiki.example.ch/vision";

    @Test
    void updateBusinessContextSavesAndReturnsPiWithNewTexts() {
        PiLink vision = new PiLink(UUID.randomUUID(), "Vision", VISION_URL);
        ProgramIncrement stored = new ProgramIncrement(
                UUID.randomUUID(), "PI 27.1", START, END, 2, "Alter Kontext", "Alte Ziele", List.of(vision));
        when(repository.findOne(stored.id())).thenReturn(Optional.of(stored));

        ProgramIncrement updated =
                service.updateBusinessContext(stored.id(), "Fokus auf Self-Service", "Checkout in 2 Schritten");

        verify(repository).save(updated);
        assertEquals(stored.id(), updated.id());
        assertEquals("Fokus auf Self-Service", updated.businessContext());
        assertEquals("Checkout in 2 Schritten", updated.artObjectives());
        assertEquals(List.of(vision), updated.links());
    }

    @Test
    void updateBusinessContextRejectsUnknownPi() {
        UUID unknown = UUID.randomUUID();
        when(repository.findOne(unknown)).thenReturn(Optional.empty());

        assertThrows(
                ProgramIncrementNotFoundException.class,
                () -> service.updateBusinessContext(unknown, "Fokus auf Self-Service", ""));
        verify(repository, never()).save(any());
    }

    @Test
    void updateBusinessContextDoesNotSaveTooLongText() {
        ProgramIncrement stored = new ProgramIncrement(UUID.randomUUID(), "PI 27.1", START, END, 2);
        when(repository.findOne(stored.id())).thenReturn(Optional.of(stored));
        String tooLong = "x".repeat(ProgramIncrement.MAX_TEXT_LENGTH + 1);

        assertThrows(
                InvalidProgramIncrementException.class, () -> service.updateBusinessContext(stored.id(), tooLong, ""));
        verify(repository, never()).save(any());
    }

    @Test
    void addLinkSavesAndReturnsPiWithLinkAtTheEnd() {
        PiLink vision = new PiLink(UUID.randomUUID(), "Vision", VISION_URL);
        ProgramIncrement stored =
                new ProgramIncrement(UUID.randomUUID(), "PI 27.1", START, END, 2, "Kontext", "Ziele", List.of(vision));
        when(repository.findOne(stored.id())).thenReturn(Optional.of(stored));

        ProgramIncrement updated = service.addLink(stored.id(), "Roadmap", "http://wiki.example.ch/roadmap");

        verify(repository).save(updated);
        assertEquals(2, updated.links().size());
        assertEquals(vision, updated.links().get(0));
        PiLink added = updated.links().get(1);
        assertEquals("Roadmap", added.title());
        assertEquals("http://wiki.example.ch/roadmap", added.url());
        assertNotEquals(vision.id(), added.id());
        assertEquals("Kontext", updated.businessContext());
    }

    @Test
    void addLinkDoesNotSaveLinkWithoutHttpOrHttps() {
        ProgramIncrement stored = new ProgramIncrement(UUID.randomUUID(), "PI 27.1", START, END, 2);
        lenient().when(repository.findOne(stored.id())).thenReturn(Optional.of(stored));

        InvalidProgramIncrementException exception = assertThrows(
                InvalidProgramIncrementException.class,
                () -> service.addLink(stored.id(), "Vision", "javascript:alert(1)"));

        assertEquals("Die URL muss mit http:// oder https:// beginnen.", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void addLinkRejectsUnknownPi() {
        UUID unknown = UUID.randomUUID();
        when(repository.findOne(unknown)).thenReturn(Optional.empty());

        assertThrows(ProgramIncrementNotFoundException.class, () -> service.addLink(unknown, "Vision", VISION_URL));
        verify(repository, never()).save(any());
    }

    @Test
    void removeLinkSavesAndReturnsPiWithoutTheLink() {
        PiLink vision = new PiLink(UUID.randomUUID(), "Vision", VISION_URL);
        PiLink roadmap = new PiLink(UUID.randomUUID(), "Roadmap", "http://wiki.example.ch/roadmap");
        ProgramIncrement stored = new ProgramIncrement(
                UUID.randomUUID(), "PI 27.1", START, END, 2, "Kontext", "Ziele", List.of(vision, roadmap));
        when(repository.findOne(stored.id())).thenReturn(Optional.of(stored));

        ProgramIncrement updated = service.removeLink(stored.id(), vision.id());

        verify(repository).save(updated);
        assertEquals(List.of(roadmap), updated.links());
        assertEquals("Kontext", updated.businessContext());
    }

    @Test
    void removeLinkRejectsUnknownLink() {
        PiLink vision = new PiLink(UUID.randomUUID(), "Vision", VISION_URL);
        ProgramIncrement stored =
                new ProgramIncrement(UUID.randomUUID(), "PI 27.1", START, END, 2, "", "", List.of(vision));
        when(repository.findOne(stored.id())).thenReturn(Optional.of(stored));
        UUID unknownLink = UUID.randomUUID();

        assertThrows(PiLinkNotFoundException.class, () -> service.removeLink(stored.id(), unknownLink));
        verify(repository, never()).save(any());
    }

    @Test
    void removeLinkRejectsUnknownPi() {
        UUID unknown = UUID.randomUUID();
        UUID linkId = UUID.randomUUID();
        when(repository.findOne(unknown)).thenReturn(Optional.empty());

        assertThrows(ProgramIncrementNotFoundException.class, () -> service.removeLink(unknown, linkId));
        verify(repository, never()).save(any());
    }
}
