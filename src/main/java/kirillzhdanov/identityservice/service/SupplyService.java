package kirillzhdanov.identityservice.service;

import jakarta.transaction.Transactional;
import kirillzhdanov.identityservice.dto.inventory.supply.CreateSupplyRequest;
import kirillzhdanov.identityservice.dto.inventory.supply.UpdateSupplyRequest;
import kirillzhdanov.identityservice.dto.inventory.supply.SupplyDto;
import kirillzhdanov.identityservice.exception.BadRequestException;
import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.model.inventory.*;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.repository.inventory.IngredientRepository;
import kirillzhdanov.identityservice.repository.inventory.StockRepository;
import kirillzhdanov.identityservice.repository.inventory.SupplyRepository;
import kirillzhdanov.identityservice.repository.inventory.WarehouseRepository;
import kirillzhdanov.identityservice.repository.inventory.StockBatchRepository;
import kirillzhdanov.identityservice.tenant.ContextAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.time.Instant;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class SupplyService {

    private final SupplyRepository supplyRepository;
    private final WarehouseRepository warehouseRepository;
    private final IngredientRepository ingredientRepository;
    private final StockRepository stockRepository;
    private final StockBatchRepository stockBatchRepository;
    private final jakarta.persistence.EntityManager em;

    @Transactional
    public Supply create(CreateSupplyRequest req) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BadRequestException("items must not be empty");
        }
        Warehouse warehouse = warehouseRepository.findByIdAndMaster_Id(req.getWarehouseId(), masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        MasterAccount master = em.getReference(MasterAccount.class, masterId);
        Supply supply = Supply.builder()
                .master(master)
                .warehouse(warehouse)
                .supplierId(req.getSupplierId())
                .date(req.getDate())
                .notes(req.getNotes())
                .status("DRAFT")
                .build();
        supply = supplyRepository.save(supply);
        // Items
        for (CreateSupplyRequest.Item it : req.getItems()) {
            if (it.getQty() == null || it.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("qty must be > 0");
            }
            if (it.getUnitCost() != null && it.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("unitCost must be >= 0");
            }
            Ingredient ing = ingredientRepository.findByIdAndMaster_Id(it.getIngredientId(), masterId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found"));
            SupplyItem si = SupplyItem.builder()
                    .supply(supply)
                    .ingredient(ing)
                    .qty(it.getQty().setScale(6, RoundingMode.HALF_UP))
                    .unitCost(it.getUnitCost() == null ? null : it.getUnitCost().setScale(6, RoundingMode.HALF_UP))
                    .expiresAt(it.getExpiresAt())
                    .build();
            supply.getItems().add(si);
        }
        // compute total cost if any unitCost provided
        supply.setTotalCost(computeTotalCostOrNull(supply));
        return supplyRepository.save(supply);
    }

    @Transactional
    public Supply post(Long id) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        Supply supply = supplyRepository.findByIdAndMaster_Id(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Supply not found"));
        if (!"DRAFT".equalsIgnoreCase(supply.getStatus())) {
            throw new BadRequestException("Only DRAFT supplies can be posted");
        }
        if (supply.getItems() == null || supply.getItems().isEmpty()) {
            throw new BadRequestException("Supply must contain at least one item");
        }
        MasterAccount master = em.getReference(MasterAccount.class, masterId);
        for (SupplyItem item : supply.getItems()) {
            Long ingredientId = item.getIngredient().getId();
            Long warehouseId = supply.getWarehouse().getId();
            Stock stock = stockRepository.findByMaster_IdAndWarehouse_IdAndIngredient_Id(masterId, warehouseId, ingredientId)
                    .orElse(null);
            if (stock == null) {
                stock = new Stock();
                stock.setMaster(master);
                stock.setIngredient(item.getIngredient());
                stock.setWarehouse(supply.getWarehouse());
                stock.setQty(BigDecimal.ZERO);
            }
            stock.setQty(stock.getQty().add(item.getQty()));
            stockRepository.save(stock);
        }
        supply.setStatus("POSTED");
        return supplyRepository.save(supply);
    }

    /**
     * Approve supply: allowed only from DRAFT.
     */
    @Transactional
    public Supply approve(Long id) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        Supply supply = supplyRepository.findByIdAndMaster_Id(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Supply not found"));
        if (!"DRAFT".equalsIgnoreCase(supply.getStatus())) {
            throw new BadRequestException("Only DRAFT supplies can be approved");
        }
        if (supply.getItems() == null || supply.getItems().isEmpty()) {
            throw new BadRequestException("Supply must contain at least one item");
        }
        // lock total cost at approval if not set
        if (supply.getTotalCost() == null) {
            supply.setTotalCost(computeTotalCostOrNull(supply));
        }
        supply.setStatus("APPROVED");
        return supplyRepository.save(supply);
    }

    /**
     * Receive supply: allowed only from APPROVED. Creates StockBatch per item and increments stock.
     */
    @Transactional
    public Supply receive(Long id) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        Supply supply = supplyRepository.findByIdAndMaster_Id(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Supply not found"));
        if ("RECEIVED".equalsIgnoreCase(supply.getStatus())) {
            throw new BadRequestException("Supply already received");
        }
        if (!"APPROVED".equalsIgnoreCase(supply.getStatus()) && !"DRAFT".equalsIgnoreCase(supply.getStatus())) {
            throw new BadRequestException("Invalid status for receive");
        }
        if (supply.getItems() == null || supply.getItems().isEmpty()) {
            throw new BadRequestException("Supply must contain at least one item");
        }
        // If called from legacy /post on DRAFT, allow single-step DRAFT->RECEIVED for backward compatibility
        boolean singleStep = "DRAFT".equalsIgnoreCase(supply.getStatus());

        MasterAccount master = em.getReference(MasterAccount.class, masterId);
        for (SupplyItem item : supply.getItems()) {
            if (item.getQty() == null || item.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Item qty must be > 0");
            }
            // Create StockBatch
            StockBatch batch = StockBatch.builder()
                    .master(master)
                    .ingredient(item.getIngredient())
                    .warehouse(supply.getWarehouse())
                    .qtyReceived(item.getQty())
                    .unitCost(item.getUnitCost())
                    .receivedAt(Instant.now())
                    .expiresAt(item.getExpiresAt())
                    .build();
            stockBatchRepository.save(batch);

            // Upsert/increment stock
            Long ingredientId = item.getIngredient().getId();
            Long warehouseId = supply.getWarehouse().getId();
            Stock stock = stockRepository.findByMaster_IdAndWarehouse_IdAndIngredient_Id(masterId, warehouseId, ingredientId)
                    .orElse(null);
            if (stock == null) {
                stock = new Stock();
                stock.setMaster(master);
                stock.setIngredient(item.getIngredient());
                stock.setWarehouse(supply.getWarehouse());
                stock.setQty(BigDecimal.ZERO);
            }
            stock.setQty(stock.getQty().add(item.getQty()));
            stockRepository.save(stock);
        }
        supply.setStatus("RECEIVED");
        return supplyRepository.save(supply);
    }

    // --- New methods ---
    @Transactional
    public Supply update(Long id, UpdateSupplyRequest req) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        Supply supply = supplyRepository.findByIdAndMaster_Id(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Supply not found"));
        if (!"DRAFT".equalsIgnoreCase(supply.getStatus())) {
            throw new BadRequestException("Only DRAFT supplies can be edited");
        }
        if (req.getWarehouseId() != null) {
            Warehouse warehouse = warehouseRepository.findByIdAndMaster_Id(req.getWarehouseId(), masterId)
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
            supply.setWarehouse(warehouse);
        }
        if (req.getSupplierId() != null) supply.setSupplierId(req.getSupplierId());
        if (req.getDate() != null) supply.setDate(req.getDate());
        if (req.getNotes() != null) supply.setNotes(req.getNotes());
        // Replace items if provided
        if (req.getItems() != null) {
            if (req.getItems().isEmpty()) {
                throw new BadRequestException("items must not be empty");
            }
            supply.getItems().clear();
            for (UpdateSupplyRequest.Item it : req.getItems()) {
                if (it.getQty() == null || it.getQty().compareTo(BigDecimal.ZERO) <= 0) throw new BadRequestException("qty must be > 0");
                if (it.getUnitCost() != null && it.getUnitCost().compareTo(BigDecimal.ZERO) < 0) throw new BadRequestException("unitCost must be >= 0");
                Ingredient ing = ingredientRepository.findByIdAndMaster_Id(it.getIngredientId(), masterId)
                        .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found"));
                SupplyItem si = SupplyItem.builder()
                        .supply(supply)
                        .ingredient(ing)
                        .qty(it.getQty().setScale(6, RoundingMode.HALF_UP))
                        .unitCost(it.getUnitCost() == null ? null : it.getUnitCost().setScale(6, RoundingMode.HALF_UP))
                        .expiresAt(it.getExpiresAt())
                        .build();
                supply.getItems().add(si);
            }
        }
        supply.setTotalCost(computeTotalCostOrNull(supply));
        return supplyRepository.save(supply);
    }

    @Transactional
    public Supply get(Long id) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        return supplyRepository.findByIdAndMaster_Id(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Supply not found"));
    }

    @Transactional
    public SupplyDto getDto(Long id) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        Supply supply = supplyRepository.findDetailedByIdAndMaster(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Supply not found"));
        return toDto(supply);
    }

    @Transactional
    public Page<SupplyDto> search(Long warehouseId, String status, Pageable pageable) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        Specification<Supply> spec = (root, q, cb) -> cb.equal(root.get("master").get("id"), masterId);
        if (warehouseId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("warehouse").get("id"), warehouseId));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("status")), status.toLowerCase()));
        }
        Sort sort = Sort.by(Sort.Order.desc("date"), Sort.Order.desc("id"));
        PageRequest pr = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return supplyRepository.findAll(spec, pr).map(this::toDto);
    }

    public SupplyDto toDto(Supply s) {
        SupplyDto dto = new SupplyDto();
        dto.setId(s.getId());
        dto.setWarehouseId(s.getWarehouse() != null ? s.getWarehouse().getId() : null);
        dto.setWarehouseName(s.getWarehouse() != null ? s.getWarehouse().getName() : null);
        dto.setSupplierId(s.getSupplierId());
        dto.setDate(s.getDate());
        dto.setNotes(s.getNotes());
        dto.setStatus(s.getStatus());
        dto.setTotalCost(s.getTotalCost());
        dto.setItems(s.getItems() == null ? java.util.List.of() : s.getItems().stream().map(it -> {
            SupplyDto.Item d = new SupplyDto.Item();
            d.setIngredientId(it.getIngredient() != null ? it.getIngredient().getId() : null);
            d.setIngredientName(it.getIngredient() != null ? it.getIngredient().getName() : null);
            d.setQty(it.getQty() != null ? it.getQty().doubleValue() : null);
            d.setUnitCost(it.getUnitCost());
            d.setExpiresAt(it.getExpiresAt());
            return d;
        }).collect(Collectors.toList()));
        return dto;
    }

    private BigDecimal computeTotalCostOrNull(Supply s) {
        if (s.getItems() == null || s.getItems().isEmpty()) return null;
        boolean anyCost = false;
        BigDecimal sum = BigDecimal.ZERO;
        for (SupplyItem it : s.getItems()) {
            if (it.getUnitCost() != null) {
                anyCost = true;
                sum = sum.add(it.getQty().multiply(it.getUnitCost()));
            }
        }
        return anyCost ? sum.setScale(6, RoundingMode.HALF_UP) : null;
    }
}
