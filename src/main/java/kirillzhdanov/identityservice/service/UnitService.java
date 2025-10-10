package kirillzhdanov.identityservice.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import kirillzhdanov.identityservice.dto.inventory.UnitDto;
import kirillzhdanov.identityservice.exception.BadRequestException;
import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.model.inventory.Unit;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.repository.inventory.UnitRepository;
import kirillzhdanov.identityservice.tenant.ContextAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UnitService {

    private final UnitRepository unitRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public UnitDto create(UnitDto dto) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        validate(dto);
        String name = dto.getName().trim();
        if (unitRepository.existsByMaster_IdAndNameIgnoreCase(masterId, name)) {
            throw new BadRequestException("Unit with this name already exists");
        }
        Unit u = new Unit();
        u.setMaster(em.getReference(MasterAccount.class, masterId));
        u.setName(name);
        u.setShortName(dto.getShortName() != null ? dto.getShortName().trim() : null);
        Unit saved = unitRepository.save(u);
        return toDto(saved);
    }

    @Transactional
    public UnitDto update(Long id, UnitDto dto) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        validate(dto);
        Unit u = unitRepository.findByIdAndMaster_Id(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
        String name = dto.getName().trim();
        if (!name.equalsIgnoreCase(u.getName()) && unitRepository.existsByMaster_IdAndNameIgnoreCase(masterId, name)) {
            throw new BadRequestException("Unit with this name already exists");
        }
        u.setName(name);
        u.setShortName(dto.getShortName() != null ? dto.getShortName().trim() : null);
        Unit saved = unitRepository.save(u);
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        Unit u = unitRepository.findByIdAndMaster_Id(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
        unitRepository.delete(u);
    }

    @Transactional
    public List<UnitDto> list() {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        return unitRepository.findAllByMaster_Id(masterId)
                .stream().map(this::toDto)
                .collect(Collectors.toList());
    }

    private void validate(UnitDto dto) {
        if (dto == null || dto.getName() == null || dto.getName().isBlank()) {
            throw new BadRequestException("Unit name is required");
        }
    }

    private UnitDto toDto(Unit u) {
        return new UnitDto(u.getId(), u.getName(), u.getShortName());
    }
}
