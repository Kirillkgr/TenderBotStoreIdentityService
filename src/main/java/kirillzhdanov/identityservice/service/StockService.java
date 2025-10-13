package kirillzhdanov.identityservice.service;

import jakarta.transaction.Transactional;
import kirillzhdanov.identityservice.dto.inventory.StockAdjustRequest;
import kirillzhdanov.identityservice.dto.inventory.StockRowDto;
import kirillzhdanov.identityservice.exception.BadRequestException;
import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.model.inventory.Ingredient;
import kirillzhdanov.identityservice.model.inventory.Stock;
import kirillzhdanov.identityservice.model.inventory.Warehouse;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.repository.inventory.IngredientRepository;
import kirillzhdanov.identityservice.repository.inventory.StockRepository;
import kirillzhdanov.identityservice.repository.inventory.SupplyItemRepository;
import kirillzhdanov.identityservice.repository.inventory.WarehouseRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.tenant.ContextAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final SupplyItemRepository supplyItemRepository;
    private final IngredientRepository ingredientRepository;
    private final WarehouseRepository warehouseRepository;
    private final MasterAccountRepository masterAccountRepository;

    @Transactional
    public List<StockRowDto> listByWarehouse(Long warehouseId) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        List<Stock> rows = stockRepository.findAllByWarehouse_IdAndMaster_Id(warehouseId, masterId);
        return rows.stream().map(s -> toDtoWithAgg(s, warehouseId, masterId)).collect(Collectors.toList());
    }

    @Transactional
    public List<StockRowDto> list(Long ingredientId, Long warehouseId) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        if (ingredientId == null && warehouseId == null) {
            throw new BadRequestException("Укажите хотя бы один фильтр: ingredientId или warehouseId");
        }
        List<Stock> rows = new ArrayList<>();
        if (ingredientId != null && warehouseId != null) {
            Optional<Stock> one = stockRepository.findByMaster_IdAndWarehouse_IdAndIngredient_Id(masterId, warehouseId, ingredientId);
            one.ifPresent(rows::add);
        } else if (warehouseId != null) {
            rows = stockRepository.findAllByWarehouse_IdAndMaster_Id(warehouseId, masterId);
        } else {
            rows = stockRepository.findAllByIngredient_IdAndMaster_Id(ingredientId, masterId);
        }
        return rows.stream().map(s -> toDtoWithAgg(s, s.getWarehouse().getId(), masterId)).collect(Collectors.toList());
    }

    @Transactional
    public StockRowDto increaseStock(StockAdjustRequest req) {
        if (req.getQty() == null || req.getQty() < 0) {
            throw new BadRequestException("qty должен быть >= 0");
        }
        return adjust(req.getIngredientId(), req.getWarehouseId(), BigDecimal.valueOf(req.getQty()));
    }

    @Transactional
    public StockRowDto decreaseStock(StockAdjustRequest req) {
        if (req.getQty() == null || req.getQty() < 0) {
            throw new BadRequestException("qty должен быть >= 0");
        }
        return adjust(req.getIngredientId(), req.getWarehouseId(), BigDecimal.valueOf(req.getQty()).negate());
    }

    private StockRowDto toDtoWithAgg(Stock s, Long warehouseId, Long masterId) {
        Ingredient ing = s.getIngredient();
        Long unitId = ing.getUnit() != null ? ing.getUnit().getId() : null;
        String unitName = ing.getUnit() != null ? (ing.getUnit().getShortName() != null ? ing.getUnit().getShortName() : ing.getUnit().getName()) : null;
        var minExpiry = supplyItemRepository.findMinExpiry(ing.getId(), warehouseId, masterId).orElse(null);
        var lastSupply = supplyItemRepository.findLastSupplyDate(ing.getId(), warehouseId, masterId).orElse(null);
        String minExpiryStr = minExpiry != null ? minExpiry.toString() : null; // yyyy-MM-dd
        String lastSupplyStr = lastSupply != null ? lastSupply.toLocalDate().toString() : null; // yyyy-MM-dd
        return new StockRowDto(
                ing.getId(),
                ing.getName(),
                unitId,
                unitName,
                ing.getPackageSize(),
                s.getQty(),
                minExpiryStr,
                lastSupplyStr,
                null, // lastUseDate
                null, // supplierName
                null  // categoryName
        );
    }

    private StockRowDto adjust(Long ingredientId, Long warehouseId, BigDecimal delta) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        Ingredient ing = ingredientRepository.findByIdAndMaster_Id(ingredientId, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Ингредиент не найден"));
        Warehouse wh = warehouseRepository.findByIdAndMaster_Id(warehouseId, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Склад не найден"));
        MasterAccount master = masterAccountRepository.findById(masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Master не найден"));

        Stock stock = stockRepository.findByMaster_IdAndWarehouse_IdAndIngredient_Id(masterId, warehouseId, ingredientId)
                .orElse(null);
        if (stock == null) {
            if (delta.signum() < 0) {
                throw new BadRequestException("Недостаточно остатка для списания");
            }
            stock = new Stock();
            stock.setMaster(master);
            stock.setIngredient(ing);
            stock.setWarehouse(wh);
            stock.setQty(BigDecimal.ZERO);
        }

        BigDecimal newQty = stock.getQty().add(delta);
        if (newQty.signum() < 0) {
            throw new BadRequestException("Недостаточно остатка для списания");
        }
        stock.setQty(newQty);
        Stock saved = stockRepository.save(stock);
        return toDtoWithAgg(saved, warehouseId, masterId);
    }
}
