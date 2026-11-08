package ch.grudligstrasse.pikel.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "milestone")
public class MilestoneEntity {

    @Id
    public UUID id;

    @Column(name = "program_increment_id", nullable = false)
    public UUID programIncrementId;

    @Column(nullable = false, length = 100)
    public String name;

    @Column(name = "milestone_date", nullable = false)
    public LocalDate date;
}
