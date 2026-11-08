package ch.grudligstrasse.pikel.application.service;

import ch.grudligstrasse.pikel.domain.model.Feature;
import ch.grudligstrasse.pikel.domain.model.FeatureCsv;
import ch.grudligstrasse.pikel.domain.model.FeatureNotFoundException;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrement;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pikel.domain.model.Scheduling;
import ch.grudligstrasse.pikel.domain.model.TeamNotFoundException;
import ch.grudligstrasse.pikel.domain.port.in.FeatureUseCase;
import ch.grudligstrasse.pikel.domain.port.out.FeatureRepository;
import ch.grudligstrasse.pikel.domain.port.out.ProgramIncrementRepository;
import ch.grudligstrasse.pikel.domain.port.out.TeamRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FeatureService implements FeatureUseCase {

    /** Nach WSJF-Rang (ohne Rang am Ende), bei Gleichstand nach Titel ohne Gross-/Kleinschreibung. */
    private static final Comparator<Feature> BACKLOG_ORDER = Comparator.comparingInt(
                    (Feature feature) -> feature.wsjfRank().orElse(Integer.MAX_VALUE))
            .thenComparing(Feature::title, String.CASE_INSENSITIVE_ORDER);

    private final FeatureRepository features;
    private final ProgramIncrementRepository programIncrements;
    private final TeamRepository teams;

    public FeatureService(
            FeatureRepository features, ProgramIncrementRepository programIncrements, TeamRepository teams) {
        this.features = features;
        this.programIncrements = programIncrements;
        this.teams = teams;
    }

    @Override
    public List<Feature> listBacklog(UUID programIncrementId) {
        ensureProgramIncrementExists(programIncrementId);
        return features.findByProgramIncrement(programIncrementId).stream()
                .sorted(BACKLOG_ORDER)
                .toList();
    }

    @Override
    @Transactional
    public Feature add(
            UUID programIncrementId,
            String title,
            String description,
            Integer wsjfRank,
            String owner,
            Integer estimate) {
        ensureProgramIncrementExists(programIncrementId);
        Feature feature = Feature.create(programIncrementId, title, description, wsjfRank, owner, estimate);
        features.save(feature);
        return feature;
    }

    @Override
    @Transactional
    public Feature update(
            UUID programIncrementId,
            UUID featureId,
            String title,
            String description,
            Integer wsjfRank,
            String owner,
            Integer estimate) {
        Feature revised = findInProgramIncrement(programIncrementId, featureId)
                .revise(title, description, wsjfRank, owner, estimate);
        features.save(revised);
        return revised;
    }

    @Override
    @Transactional
    public void remove(UUID programIncrementId, UUID featureId) {
        findInProgramIncrement(programIncrementId, featureId);
        features.remove(featureId);
    }

    @Override
    @Transactional
    public Feature schedule(UUID programIncrementId, UUID featureId, UUID teamId, Integer iterationNumber) {
        ProgramIncrement programIncrement =
                programIncrements.findOne(programIncrementId).orElseThrow(ProgramIncrementNotFoundException::new);
        Feature feature = findInProgramIncrement(programIncrementId, featureId);
        // Erst PI und Feature laden (404), dann fehlende Angaben (400); ein Team lässt sich nur mit Angabe suchen.
        Scheduling cell = Scheduling.of(teamId, iterationNumber);
        if (teams.findOne(cell.teamId()).isEmpty()) {
            throw new TeamNotFoundException();
        }
        Feature scheduled = feature.schedule(programIncrement, cell.teamId(), cell.iterationNumber());
        features.save(scheduled);
        return scheduled;
    }

    @Override
    @Transactional
    public Feature unschedule(UUID programIncrementId, UUID featureId) {
        Feature feature = findInProgramIncrement(programIncrementId, featureId);
        if (!feature.scheduled()) {
            return feature;
        }
        Feature unscheduled = feature.unschedule();
        features.save(unscheduled);
        return unscheduled;
    }

    @Override
    @Transactional
    public List<Feature> importCsv(UUID programIncrementId, String csv) {
        ensureProgramIncrementExists(programIncrementId);
        // Alles oder nichts: Erst die ganze Datei prüfen, dann speichern.
        List<Feature> imported = FeatureCsv.parse(programIncrementId, csv);
        imported.forEach(features::save);
        return imported;
    }

    // Ein Feature eines anderen PI ist unter dieser Adresse unbekannt, sonst liesse es sich über jedes PI ändern.
    private Feature findInProgramIncrement(UUID programIncrementId, UUID featureId) {
        ensureProgramIncrementExists(programIncrementId);
        return features.findOne(featureId)
                .filter(feature -> feature.programIncrementId().equals(programIncrementId))
                .orElseThrow(FeatureNotFoundException::new);
    }

    private void ensureProgramIncrementExists(UUID programIncrementId) {
        if (programIncrements.findOne(programIncrementId).isEmpty()) {
            throw new ProgramIncrementNotFoundException();
        }
    }
}
