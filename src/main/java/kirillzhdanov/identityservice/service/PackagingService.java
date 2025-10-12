package kirillzhdanov.identityservice.service;

import jakarta.transaction.Transactional;
import kirillzhdanov.identityservice.dto.inventory.packaging.PackagingDto;
import kirillzhdanov.identityservice.exception.BadRequestException;
import kirillzhdanov.identityservice.exception.ResourceAlreadyExistsException;
import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.model.inventory.Packaging;
import kirillzhdanov.identityservice.model.inventory.Unit;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.repository.inventory.PackagingRepository;
import kirillzhdanov.identityservice.repository.inventory.UnitRepository;
import kirillzhdanov.identityservice.tenant.ContextAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PackagingService {

    private final PackagingRepository packagingRepository;
    private final UnitRepository unitRepository;
    private final jakarta.persistence.EntityManager em;

    @Transactional
    public List<PackagingDto> list() {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        return packagingRepository.findAllByMaster_Id(masterId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public PackagingDto create(PackagingDto req) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        if (req.getSize() == null || req.getSize() < 0) throw new BadRequestException("size must be >= 0");
        if (req.getName() == null || req.getName().isBlank()) throw new BadRequestException("name is required");
        if (req.getUnitId() == null) throw new BadRequestException("unitId is required");
        if (packagingRepository.existsByMaster_IdAndNameIgnoreCase(masterId, req.getName())) {
            throw new ResourceAlreadyExistsException("Packaging with name already exists");
        }
        Unit unit = unitRepository.findByIdAndMaster_Id(req.getUnitId(), masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
        MasterAccount master = em.getReference(MasterAccount.class, masterId);
        Packaging p = Packaging.builder()
                .master(master)
                .name(req.getName().trim())
                .unit(unit)
                .size(BigDecimal.valueOf(req.getSize()))
                .build();
        Packaging saved = packagingRepository.save(p);
        return toDto(saved);
    }

    @Transactional
    public PackagingDto update(Long id, PackagingDto req) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        Packaging p = packagingRepository.findByIdAndMaster_Id(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Packaging not found"));
        if (req.getName() != null) p.setName(req.getName().trim());
        if (req.getUnitId() != null) {
            Unit u = unitRepository.findByIdAndMaster_Id(req.getUnitId(), masterId)
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
            p.setUnit(u);
        }
        if (req.getSize() != null) {
            if (req.getSize() < 0) throw new BadRequestException("size must be >= 0");
            p.setSize(BigDecimal.valueOf(req.getSize()));
        }
        return toDto(packagingRepository.save(p));
    }

    @Transactional
    public void delete(Long id) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        Packaging p = packagingRepository.findByIdAndMaster_Id(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Packaging not found"));
        packagingRepository.delete(p);
    }

    private PackagingDto toDto(Packaging p) {
        return new PackagingDto(
                p.getId(),
                p.getName(),
                p.getUnit() != null ? p.getUnit().getId() : null,
                p.getSize() != null ? p.getSize().doubleValue() : null
        );
    }
}
