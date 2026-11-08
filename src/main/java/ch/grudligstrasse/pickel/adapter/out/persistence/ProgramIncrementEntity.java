package ch.grudligstrasse.pickel.adapter.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "program_increment")
public class ProgramIncrementEntity {

    @Id
    public UUID id;

    @Column(nullable = false, length = 100)
    public String name;

    @Column(name = "start_date", nullable = false)
    public LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    public LocalDate endDate;

    @Column(name = "iteration_length_weeks", nullable = false)
    public int iterationLengthWeeks;

    @Column(name = "business_context", nullable = false, columnDefinition = "TEXT")
    public String businessContext;

    @Column(name = "art_objectives", nullable = false, columnDefinition = "TEXT")
    public String artObjectives;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "program_increment_id", nullable = false)
    @OrderColumn(name = "position", nullable = false)
    public List<PiLinkEntity> links = new ArrayList<>();
}
