package ch.grudligstrasse.pickel.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "capacity")
@IdClass(CapacityEntity.Key.class)
public class CapacityEntity {

    @Id
    @Column(name = "program_increment_id")
    public UUID programIncrementId;

    @Id
    @Column(name = "team_id")
    public UUID teamId;

    @Id
    @Column(name = "iteration_number")
    public int iterationNumber;

    @Column(nullable = false)
    public int points;

    /** Zusammengesetzter Schlüssel; Namen und Typen wie die {@code @Id}-Felder. */
    public record Key(UUID programIncrementId, UUID teamId, int iterationNumber) implements Serializable {}
}
