package ch.grudligstrasse.pikel.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TeamTest {

    private static final String BLANK_MESSAGE = "Der Teamname darf nicht leer sein.";
    private static final String TOO_LONG_MESSAGE = "Der Teamname darf höchstens 50 Zeichen lang sein.";
    private static final int FIFTY = 50;

    @Test
    void createTrimsName() {
        assertEquals("Rakete", Team.create("  Rakete ").name());
    }

    @Test
    void createAssignsOwnIdToEachTeam() {
        Team first = Team.create("Rakete");
        Team second = Team.create("Komet");

        assertNotNull(first.id());
        assertNotEquals(first.id(), second.id());
    }

    @Test
    void acceptsNameOfExactlyFiftyCharacters() {
        String name = "x".repeat(FIFTY);

        assertEquals(name, Team.create(name).name());
    }

    // Die Länge zählt nach dem Trimmen: Leerzeichen am Rand machen einen Namen nicht zu lang.
    @Test
    void acceptsFiftyCharactersSurroundedByBlanks() {
        String name = "x".repeat(FIFTY);

        assertEquals(name, Team.create("  " + name + "  ").name());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t"})
    void rejectsBlankName(String name) {
        InvalidTeamException exception = assertThrows(InvalidTeamException.class, () -> Team.create(name));

        assertEquals(BLANK_MESSAGE, exception.getMessage());
    }

    // AK 1 «Pflicht»: ein fehlender Name (JSON ohne Feld) ist wie ein leerer Name.
    @Test
    void rejectsMissingName() {
        InvalidTeamException exception = assertThrows(InvalidTeamException.class, () -> Team.create(null));

        assertEquals(BLANK_MESSAGE, exception.getMessage());
    }

    @Test
    void rejectsNameLongerThanFiftyCharacters() {
        String name = "x".repeat(FIFTY + 1);

        InvalidTeamException exception = assertThrows(InvalidTeamException.class, () -> Team.create(name));

        assertEquals(TOO_LONG_MESSAGE, exception.getMessage());
    }

    @Test
    void renameKeepsIdAndTrimsNewName() {
        UUID id = UUID.randomUUID();
        Team team = new Team(id, "Rakete");

        Team renamed = team.rename(" Komet  ");

        assertEquals(new Team(id, "Komet"), renamed);
    }

    @Test
    void renameRejectsBlankName() {
        Team team = new Team(UUID.randomUUID(), "Rakete");

        InvalidTeamException exception = assertThrows(InvalidTeamException.class, () -> team.rename("  "));

        assertEquals(BLANK_MESSAGE, exception.getMessage());
    }

    @Test
    void renameRejectsNameLongerThanFiftyCharacters() {
        Team team = new Team(UUID.randomUUID(), "Rakete");
        String name = "x".repeat(FIFTY + 1);

        InvalidTeamException exception = assertThrows(InvalidTeamException.class, () -> team.rename(name));

        assertEquals(TOO_LONG_MESSAGE, exception.getMessage());
    }

    @Test
    void renameAcceptsNameOfExactlyFiftyCharacters() {
        Team team = new Team(UUID.randomUUID(), "Rakete");
        String name = "y".repeat(FIFTY);

        assertEquals(name, team.rename(name).name());
    }
}
