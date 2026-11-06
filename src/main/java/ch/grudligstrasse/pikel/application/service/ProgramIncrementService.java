package ch.grudligstrasse.pikel.application.service;

import ch.grudligstrasse.pikel.domain.model.ProgramIncrement;
import ch.grudligstrasse.pikel.domain.port.in.ProgramIncrementUseCase;
import ch.grudligstrasse.pikel.domain.port.out.ProgramIncrementRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;

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
    public ProgramIncrement plan(String name, LocalDate startDate, LocalDate endDate) {
        ProgramIncrement programIncrement = ProgramIncrement.plan(name, startDate, endDate);
        repository.save(programIncrement);
        return programIncrement;
    }
}
