-- ON DELETE CASCADE: Ein gelöschtes Feature nimmt seine Abhängigkeiten in beide Richtungen mit.
CREATE TABLE feature_dependency (
    needing_feature_id  UUID  NOT NULL REFERENCES feature (id) ON DELETE CASCADE,
    needed_feature_id   UUID  NOT NULL REFERENCES feature (id) ON DELETE CASCADE,
    PRIMARY KEY (needing_feature_id, needed_feature_id)
);

CREATE INDEX feature_dependency_needed_idx ON feature_dependency (needed_feature_id);
