package ch.grudligstrasse.pickel.domain.model;

/** Es gibt schon ein Team mit diesem Namen (ohne Rücksicht auf Gross- und Kleinschreibung). */
public class DuplicateTeamNameException extends InvalidTeamException {

    public DuplicateTeamNameException() {
        super("Ein Team mit diesem Namen gibt es schon.");
    }
}
