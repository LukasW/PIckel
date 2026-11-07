package ch.grudligstrasse.pikel.application.service;

import ch.grudligstrasse.pikel.domain.model.PiLink;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrement;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pikel.domain.port.in.ProgramIncrementUseCase;
import ch.grudligstrasse.pikel.domain.port.out.ProgramIncrementRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProgramIncrementService implements ProgramIncrementUseCase {

    private final ProgramIncrementRepository repository;

    public ProgramIncrementService(ProgramIncrementRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ProgramIncrement> listAll() {
        return repository.findAllOrderedByStartDate();
    }

    @Override
    @Transactional
    public ProgramIncrement plan(String name, LocalDate startDate, LocalDate endDate, Integer iterationLengthWeeks) {
        ProgramIncrement programIncrement = ProgramIncrement.plan(name, startDate, endDate, iterationLengthWeeks);
        repository.save(programIncrement);
        return programIncrement;
    }

    @Override
    public ProgramIncrement get(UUID id) {
        return repository.findOne(id).orElseThrow(ProgramIncrementNotFoundException::new);
    }

    @Override
    @Transactional
    public ProgramIncrement updateBusinessContext(UUID id, String businessContext, String artObjectives) {
        return saved(get(id).withBusinessContext(businessContext, artObjectives));
    }

    @Override
    @Transactional
    public ProgramIncrement addLink(UUID id, String title, String url) {
        return saved(get(id).withLink(PiLink.create(title, url)));
    }

    @Override
    @Transactional
    public ProgramIncrement removeLink(UUID id, UUID linkId) {
        return saved(get(id).withoutLink(linkId));
    }

    private ProgramIncrement saved(ProgramIncrement programIncrement) {
        repository.save(programIncrement);
        return programIncrement;
    }
}
