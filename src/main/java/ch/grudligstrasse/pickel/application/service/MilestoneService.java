package ch.grudligstrasse.pickel.application.service;

import ch.grudligstrasse.pickel.domain.model.Milestone;
import ch.grudligstrasse.pickel.domain.model.MilestoneNotFoundException;
import ch.grudligstrasse.pickel.domain.model.ProgramIncrement;
import ch.grudligstrasse.pickel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pickel.domain.port.in.MilestoneUseCase;
import ch.grudligstrasse.pickel.domain.port.out.MilestoneRepository;
import ch.grudligstrasse.pickel.domain.port.out.ProgramIncrementRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class MilestoneService implements MilestoneUseCase {

    private final MilestoneRepository milestones;
    private final ProgramIncrementRepository programIncrements;

    public MilestoneService(MilestoneRepository milestones, ProgramIncrementRepository programIncrements) {
        this.milestones = milestones;
        this.programIncrements = programIncrements;
    }

    // Die Reihenfolge (Datum, dann Name) liefert der Port; hier nicht ein zweites Mal sortieren.
    @Override
    public List<Milestone> list(UUID programIncrementId) {
        programIncrement(programIncrementId);
        return milestones.findByProgramIncrement(programIncrementId);
    }

    @Override
    @Transactional
    public Milestone add(UUID programIncrementId, String name, LocalDate date) {
        Milestone milestone = Milestone.create(programIncrement(programIncrementId), name, date);
        milestones.save(milestone);
        return milestone;
    }

    @Override
    @Transactional
    public Milestone update(UUID programIncrementId, UUID milestoneId, String name, LocalDate date) {
        ProgramIncrement programIncrement = programIncrement(programIncrementId);
        Milestone updated = milestone(programIncrementId, milestoneId).update(programIncrement, name, date);
        milestones.save(updated);
        return updated;
    }

    @Override
    @Transactional
    public void delete(UUID programIncrementId, UUID milestoneId) {
        programIncrement(programIncrementId);
        milestones.delete(milestone(programIncrementId, milestoneId).id());
    }

    private ProgramIncrement programIncrement(UUID id) {
        return programIncrements.findOne(id).orElseThrow(ProgramIncrementNotFoundException::new);
    }

    private Milestone milestone(UUID programIncrementId, UUID milestoneId) {
        return milestones.findOne(programIncrementId, milestoneId).orElseThrow(MilestoneNotFoundException::new);
    }
}
