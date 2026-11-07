package ch.grudligstrasse.pikel.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "pi_link")
public class PiLinkEntity {

    @Id
    public UUID id;

    @Column(nullable = false, length = 100)
    public String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String url;
}
