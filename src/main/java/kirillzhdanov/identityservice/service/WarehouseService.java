package kirillzhdanov.identityservice.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import kirillzhdanov.identityservice.dto.inventory.WarehouseDto;
import kirillzhdanov.identityservice.exception.BadRequestException;
import kirillzhdanov.identityservice.exception.ResourceAlreadyExistsException;
import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.model.inventory.Warehouse;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.repository.inventory.WarehouseRepository;
import kirillzhdanov.identityservice.tenant.ContextAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public WarehouseDto create(WarehouseDto dto) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BadRequestException("Warehouse name is required");
        }
        String name = dto.getName().trim();
        if (warehouseRepository.existsByMaster_IdAndNameIgnoreCase(masterId, name)) {
            throw new ResourceAlreadyExistsException("Warehouse with this name already exists");
        }
        Warehouse saved = warehouseRepository.save(new Warehouse(em.getReference(MasterAccount.class, masterId), name));
        return toDto(saved);
    }

    @Transactional
    public WarehouseDto update(Long id, WarehouseDto dto) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        Warehouse w = warehouseRepository.findByIdAndMaster_Id(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BadRequestException("Warehouse name is required");
        }
        String name = dto.getName().trim();
        if (!name.equalsIgnoreCase(w.getName()) && warehouseRepository.existsByMaster_IdAndNameIgnoreCase(masterId, name)) {
            throw new ResourceAlreadyExistsException("Warehouse with this name already exists");
        }
        w.setName(name);
        Warehouse saved = warehouseRepository.save(w);
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        Warehouse w = warehouseRepository.findByIdAndMaster_Id(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        warehouseRepository.delete(w);
    }

    @Transactional
    public List<WarehouseDto> list() {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        return warehouseRepository.findAllByMaster_Id(masterId)
                .stream().map(this::toDto)
                .collect(Collectors.toList());
    }

    private WarehouseDto toDto(Warehouse w) {
        return new WarehouseDto(w.getId(), w.getName());
    }
}
