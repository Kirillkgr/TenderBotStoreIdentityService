package kirillzhdanov.identityservice.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import kirillzhdanov.identityservice.dto.inventory.CreateIngredientRequest;
import kirillzhdanov.identityservice.dto.inventory.IngredientDto;
import kirillzhdanov.identityservice.dto.inventory.IngredientWithStockDto;
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
import kirillzhdanov.identityservice.repository.inventory.WarehouseRepository;
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
    private final WarehouseRepository warehouseRepository;
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
    public List<IngredientWithStockDto> listWithStockAll() {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        String ql = "select i.id, i.name, u.id, u.name, i.packageSize, i.notes, " +
                " s.warehouse.id, s.qty, s.warehouse.name, " +
                " (select max(su.date) from SupplyItem si join si.supply su " +
                "   where si.ingredient = i and su.master.id = :masterId and su.warehouse = s.warehouse) " +
                " from Stock s " +
                " join s.ingredient i " +
                " join i.unit u " +
                " where s.master.id = :masterId";
        var rows = em.createQuery(ql, Object[].class)
                .setParameter("masterId", masterId)
                .getResultList();
        return rows.stream().map(r -> {
            Long id = (Long) r[0];
            String name = (String) r[1];
            Long unitId = (Long) r[2];
            String unitName = (String) r[3];
            java.math.BigDecimal packageSize = (java.math.BigDecimal) r[4];
            String notes = (String) r[5];
            Long warehouseId = (Long) r[6];
            java.math.BigDecimal qty = (java.math.BigDecimal) r[7];
            String warehouseName = (String) r[8];
            java.time.OffsetDateTime lastDate = (java.time.OffsetDateTime) r[9];
            String lastIso = lastDate != null ? lastDate.toString() : null;

            var dto = new IngredientWithStockDto();
            dto.setId(id);
            dto.setName(name);
            dto.setUnitId(unitId);
            dto.setUnitName(unitName);
            dto.setPackageSize(packageSize);
            dto.setNotes(notes);
            dto.setWarehouseId(warehouseId);
            dto.setQuantity(qty != null ? qty : java.math.BigDecimal.ZERO);
            dto.setWarehouseName(warehouseName);
            dto.setLastSupplyDate(lastIso);
            return dto;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public List<IngredientWithStockDto> listWithStock(Long warehouseId) {
        if (warehouseId == null) {
            throw new BadRequestException("warehouseId is required");
        }
        Long masterId = ContextAccess.getMasterIdOrThrow();
        // Validate warehouse belongs to current master and get its name
        var warehouse = warehouseRepository.findByIdAndMaster_Id(warehouseId, masterId)
                .orElseThrow(() -> new BadRequestException("Warehouse does not exist in current master"));

        // Select only existing stock rows for this warehouse (same behavior as listWithStockAll)
        String ql = "select i.id, i.name, u.id, u.name, i.packageSize, i.notes, " +
                " s.qty, suMax.date " +
                " from Stock s " +
                " join s.ingredient i " +
                " join i.unit u " +
                " left join (select si2.ingredient id2, max(su2.date) date from SupplyItem si2 join si2.supply su2 " +
                "            where su2.master.id = :masterId and su2.warehouse.id = :warehouseId group by si2.ingredient) suMax " +
                "        on suMax.id2 = i " +
                " where s.master.id = :masterId and s.warehouse.id = :warehouseId";

        var rows = em.createQuery(ql, Object[].class)
                .setParameter("warehouseId", warehouseId)
                .setParameter("masterId", masterId)
                .getResultList();

        String warehouseName = warehouse.getName();
        return rows.stream().map(r -> {
            Long id = (Long) r[0];
            String name = (String) r[1];
            Long unitId = (Long) r[2];
            String unitName = (String) r[3];
            java.math.BigDecimal packageSize = (java.math.BigDecimal) r[4];
            String notes = (String) r[5];
            java.math.BigDecimal qty = (java.math.BigDecimal) r[6];
            java.time.OffsetDateTime lastDate = (java.time.OffsetDateTime) r[7];
            String lastIso = lastDate != null ? lastDate.toString() : null;
            var dto = new IngredientWithStockDto();
            dto.setId(id);
            dto.setName(name);
            dto.setUnitId(unitId);
            dto.setUnitName(unitName);
            dto.setPackageSize(packageSize);
            dto.setNotes(notes);
            dto.setWarehouseId(warehouseId);
            dto.setQuantity(qty != null ? qty : java.math.BigDecimal.ZERO);
            dto.setWarehouseName(warehouseName);
            dto.setLastSupplyDate(lastIso);
            return dto;
        }).collect(Collectors.toList());
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
