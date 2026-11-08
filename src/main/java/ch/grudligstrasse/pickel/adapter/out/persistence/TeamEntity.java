package ch.grudligstrasse.pickel.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "team")
public class TeamEntity {

    @Id
    public UUID id;

    @Column(nullable = false, length = 50)
    public String name;
}
