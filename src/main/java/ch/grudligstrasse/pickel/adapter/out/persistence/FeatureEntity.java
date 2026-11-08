package ch.grudligstrasse.pickel.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "feature")
public class FeatureEntity {

    @Id
    public UUID id;

    @Column(name = "program_increment_id", nullable = false)
    public UUID programIncrementId;

    @Column(nullable = false, length = 200)
    public String title;

    @Column(nullable = false, length = 5000)
    public String description;

    @Column(nullable = false, length = 100)
    public String owner;

    @Column(name = "wsjf_rank")
    public Integer wsjfRank;

    public Integer estimate;

    @Column(name = "team_id")
    public UUID teamId;

    @Column(name = "iteration_number")
    public Integer iterationNumber;
}
