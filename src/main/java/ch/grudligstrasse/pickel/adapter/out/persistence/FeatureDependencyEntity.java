package ch.grudligstrasse.pickel.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "feature_dependency")
@IdClass(FeatureDependencyEntity.Key.class)
public class FeatureDependencyEntity {

    @Id
    @Column(name = "needing_feature_id")
    public UUID needingFeatureId;

    @Id
    @Column(name = "needed_feature_id")
    public UUID neededFeatureId;

    public record Key(UUID needingFeatureId, UUID neededFeatureId) implements Serializable {}
}
