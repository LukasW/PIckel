package ch.grudligstrasse.pikel.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.grudligstrasse.pikel.domain.model.DuplicateTeamNameException;
import ch.grudligstrasse.pikel.domain.model.InvalidTeamException;
import ch.grudligstrasse.pikel.domain.model.Team;
import ch.grudligstrasse.pikel.domain.model.TeamNotFoundException;
import ch.grudligstrasse.pikel.domain.port.out.FeatureRepository;
import ch.grudligstrasse.pikel.domain.port.out.TeamRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    private static final String DUPLICATE_MESSAGE = "Ein Team mit diesem Namen gibt es schon.";
    private static final String BLANK_MESSAGE = "Der Teamname darf nicht leer sein.";

    @Mock
    TeamRepository repository;

    // US-13: Entfernen gibt die eingeplanten Features des Teams zurück ins Backlog.
    @Mock
    FeatureRepository features;

    @InjectMocks
    TeamService service;

    private static Team team(String name) {
        return new Team(UUID.randomUUID(), name);
    }

    // Der Port sucht ohne Rücksicht auf Gross- und Kleinschreibung; der Stub bildet diesen Vertrag nach.
    // lenient: Ob der Service beim Umbenennen auf den eigenen Namen überhaupt sucht, ist seine Sache.
    private void nameIsTakenBy(Team existing) {
        lenient()
                .when(repository.findByNameIgnoreCase(
                        argThat(name -> existing.name().equalsIgnoreCase(name))))
                .thenReturn(Optional.of(existing));
    }

    // --- AK 3: alphabetisch sortiert ---

    // Natürliche String-Ordnung ergäbe «Biber, Zebra, adler»; verlangt ist alphabetisch ohne Gross/Klein.
    @Test
    void listAllSortsAlphabeticallyIgnoringCase() {
        Team zebra = team("Zebra");
        Team adler = team("adler");
        Team biber = team("Biber");
        when(repository.findAllTeams()).thenReturn(List.of(zebra, adler, biber));

        assertEquals(List.of(adler, biber, zebra), service.listAll());
    }

    // Plan: Collator de-CH; ein Umlaut steht bei seinem Grundbuchstaben, nicht hinter «Z».
    @Test
    void listAllSortsUmlautWithItsBaseLetter() {
        Team zebra = team("Zebra");
        Team aerger = team("Ärger");
        Team biber = team("Biber");
        Team adler = team("Adler");
        when(repository.findAllTeams()).thenReturn(List.of(zebra, aerger, biber, adler));

        assertEquals(List.of(adler, aerger, biber, zebra), service.listAll());
    }

    @Test
    void listAllWithoutTeamsIsEmpty() {
        when(repository.findAllTeams()).thenReturn(List.of());

        assertEquals(List.of(), service.listAll());
    }

    // --- AK 1 und 2: Erfassen ---

    @Test
    void addSavesAndReturnsTeamWithTrimmedName() {
        Team created = service.add("  Rakete ");

        assertEquals("Rakete", created.name());
        verify(repository).save(created);
    }

    @Test
    void addDoesNotSaveBlankName() {
        InvalidTeamException exception = assertThrows(InvalidTeamException.class, () -> service.add(" "));

        assertEquals(BLANK_MESSAGE, exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void addRejectsNameOfExistingTeamInOtherCase() {
        nameIsTakenBy(team("Rakete"));

        DuplicateTeamNameException exception =
                assertThrows(DuplicateTeamNameException.class, () -> service.add("rakete"));

        assertEquals(DUPLICATE_MESSAGE, exception.getMessage());
        verify(repository, never()).save(any());
    }

    // Eindeutig ist der gespeicherte, also getrimmte Name.
    @Test
    void addRejectsNameOfExistingTeamSurroundedByBlanks() {
        nameIsTakenBy(team("Rakete"));

        assertThrows(DuplicateTeamNameException.class, () -> service.add("  RAKETE "));
        verify(repository, never()).save(any());
    }

    // --- AK 4: Umbenennen mit denselben Regeln ---

    @Test
    void renameSavesTeamWithNewTrimmedNameAndSameId() {
        Team rakete = team("Rakete");
        when(repository.findOne(rakete.id())).thenReturn(Optional.of(rakete));

        Team renamed = service.rename(rakete.id(), " Komet ");

        assertEquals(new Team(rakete.id(), "Komet"), renamed);
        verify(repository).save(new Team(rakete.id(), "Komet"));
    }

    @Test
    void renameToOwnNameInOtherCaseIsAllowed() {
        Team rakete = team("Rakete");
        when(repository.findOne(rakete.id())).thenReturn(Optional.of(rakete));
        nameIsTakenBy(rakete);

        Team renamed = service.rename(rakete.id(), "RAKETE");

        assertEquals(new Team(rakete.id(), "RAKETE"), renamed);
        verify(repository).save(new Team(rakete.id(), "RAKETE"));
    }

    // Gegenstück zum Fall oben: Nur der Treffer der Namenssuche ist ein anderes Team.
    @Test
    void renameToNameOfOtherTeamIsRejected() {
        Team rakete = team("Rakete");
        when(repository.findOne(rakete.id())).thenReturn(Optional.of(rakete));
        nameIsTakenBy(team("Komet"));

        DuplicateTeamNameException exception =
                assertThrows(DuplicateTeamNameException.class, () -> service.rename(rakete.id(), "komet"));

        assertEquals(DUPLICATE_MESSAGE, exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void renameRejectsBlankName() {
        Team rakete = team("Rakete");
        // lenient: ob der Service vor oder nach der Namensprüfung liest, ist hier nicht Gegenstand.
        lenient().when(repository.findOne(rakete.id())).thenReturn(Optional.of(rakete));

        InvalidTeamException exception =
                assertThrows(InvalidTeamException.class, () -> service.rename(rakete.id(), "  "));

        assertEquals(BLANK_MESSAGE, exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void renameRejectsUnknownId() {
        UUID unknown = UUID.randomUUID();
        when(repository.findOne(unknown)).thenReturn(Optional.empty());

        assertThrows(TeamNotFoundException.class, () -> service.rename(unknown, "Komet"));
        verify(repository, never()).save(any());
    }

    // Annahme (notes.md): erst die Existenz, dann der Name; ein unbekanntes Team ist 404, nicht 400.
    @Test
    void renameOfUnknownTeamIsNotFoundEvenWithInvalidName() {
        UUID unknown = UUID.randomUUID();
        when(repository.findOne(unknown)).thenReturn(Optional.empty());

        assertThrows(TeamNotFoundException.class, () -> service.rename(unknown, " "));
    }

    // --- AK 5: Entfernen ---

    @Test
    void removeDeletesTeamOfId() {
        UUID id = UUID.randomUUID();
        when(repository.remove(id)).thenReturn(true);

        service.remove(id);

        verify(repository).remove(id);
    }

    // US-13 AK 6, Szenario «Entferntes Team gibt seine Features ins Backlog zurück»: in allen PIs, und zwar bevor das
    // Team gelöscht wird (Plan: der Fremdschlüssel ohne ON DELETE lässt das Löschen sonst scheitern).
    @Test
    void removeReturnsScheduledFeaturesOfTeamToBacklogBeforeDeletingTeam() {
        UUID id = UUID.randomUUID();
        when(repository.remove(id)).thenReturn(true);

        service.remove(id);

        InOrder removal = inOrder(features, repository);
        removal.verify(features).unscheduleTeam(id);
        removal.verify(repository).remove(id);
    }

    @Test
    void removeRejectsUnknownId() {
        UUID unknown = UUID.randomUUID();
        when(repository.remove(unknown)).thenReturn(false);

        assertThrows(TeamNotFoundException.class, () -> service.remove(unknown));
    }
}
