package kirillzhdanov.identityservice.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import kirillzhdanov.identityservice.dto.inventory.CreateIngredientRequest;
import kirillzhdanov.identityservice.dto.inventory.IngredientDto;
import kirillzhdanov.identityservice.dto.inventory.UpdateIngredientRequest;
import kirillzhdanov.identityservice.dto.inventory.supply.CreateSupplyRequest;
import kirillzhdanov.identityservice.exception.BadRequestException;
import kirillzhdanov.identityservice.exception.ResourceAlreadyExistsException;
import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.model.inventory.Ingredient;
import kirillzhdanov.identityservice.model.inventory.Unit;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.repository.inventory.IngredientRepository;
import kirillzhdanov.identityservice.repository.inventory.UnitRepository;
import kirillzhdanov.identityservice.tenant.ContextAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final UnitRepository unitRepository;
    private final SupplyService supplyService;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public List<IngredientDto> list() {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        return ingredientRepository.findAllByMaster_Id(masterId)
                .stream().map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public IngredientDto create(CreateIngredientRequest req) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        String name = validateName(req.getName());
        BigDecimal packageSize = validatePackageSize(req.getPackageSize());
        Long unitId = validateUnitId(req.getUnitId());

        if (ingredientRepository.existsByMaster_IdAndNameIgnoreCase(masterId, name)) {
            throw new ResourceAlreadyExistsException("Ingredient with this name already exists");
        }

        // Validate unit belongs to master
        Unit unit = unitRepository.findByIdAndMaster_Id(unitId, masterId)
                .orElseThrow(() -> new BadRequestException("Unit does not exist in current master"));

        Ingredient entity = new Ingredient();
        entity.setMaster(em.getReference(MasterAccount.class, masterId));
        entity.setName(name);
        entity.setUnit(unit);
        entity.setPackageSize(packageSize);
        entity.setNotes(trimOrNull(req.getNotes()));

        Ingredient saved = ingredientRepository.save(entity);

        // Variant A: create and post initial supply to produce stock for a warehouse
        if (req.getInitialQty() != null && req.getWarehouseId() != null) {
            if (req.getInitialQty().signum() > 0) {
                CreateSupplyRequest sreq = new CreateSupplyRequest();
                sreq.setWarehouseId(req.getWarehouseId());
                sreq.setSupplierId(null);
                sreq.setDate(java.time.OffsetDateTime.now());
                sreq.setNotes("Initial stock for ingredient " + saved.getName());

                CreateSupplyRequest.Item item = new CreateSupplyRequest.Item();
                item.setIngredientId(saved.getId());
                item.setQty(req.getInitialQty().doubleValue());
                item.setExpiresAt(null);
                sreq.setItems(java.util.List.of(item));

                var supply = supplyService.create(sreq);
                supplyService.post(supply.getId());
            }
        }

        return toDto(saved);
    }

    @Transactional
    public IngredientDto update(Long id, UpdateIngredientRequest req) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        Ingredient entity = ingredientRepository.findByIdAndMaster_Id(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found"));

        String newName = validateName(req.getName());
        BigDecimal packageSize = validatePackageSize(req.getPackageSize());
        Long unitId = validateUnitId(req.getUnitId());

        if (!newName.equalsIgnoreCase(entity.getName()) &&
                ingredientRepository.existsByMaster_IdAndNameIgnoreCase(masterId, newName)) {
            throw new ResourceAlreadyExistsException("Ingredient with this name already exists");
        }

        Unit unit = unitRepository.findByIdAndMaster_Id(unitId, masterId)
                .orElseThrow(() -> new BadRequestException("Unit does not exist in current master"));

        entity.setName(newName);
        entity.setUnit(unit);
        entity.setPackageSize(packageSize);
        entity.setNotes(trimOrNull(req.getNotes()));

        Ingredient saved = ingredientRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        Ingredient entity = ingredientRepository.findByIdAndMaster_Id(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found"));
        ingredientRepository.delete(entity);
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Ingredient name is required");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 255) {
            throw new BadRequestException("Name must be at most 255 characters");
        }
        return trimmed;
    }

    private Long validateUnitId(Long unitId) {
        if (unitId == null) {
            throw new BadRequestException("unitId is required");
        }
        return unitId;
    }

    private BigDecimal validatePackageSize(BigDecimal pkg) {
        if (pkg == null) return null;
        if (pkg.signum() < 0) {
            throw new BadRequestException("packageSize must be >= 0");
        }
        return pkg;
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private IngredientDto toDto(Ingredient e) {
        return new IngredientDto(
                e.getId(),
                e.getName(),
                e.getUnit() != null ? e.getUnit().getId() : null,
                e.getUnit() != null ? e.getUnit().getName() : null,
                e.getPackageSize(),
                e.getNotes()
        );
    }
}
