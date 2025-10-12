package kirillzhdanov.identityservice.service;

import jakarta.transaction.Transactional;
import kirillzhdanov.identityservice.dto.inventory.StockRowDto;
import kirillzhdanov.identityservice.model.inventory.Ingredient;
import kirillzhdanov.identityservice.model.inventory.Stock;
import kirillzhdanov.identityservice.repository.inventory.StockRepository;
import kirillzhdanov.identityservice.repository.inventory.SupplyItemRepository;
import kirillzhdanov.identityservice.tenant.ContextAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final SupplyItemRepository supplyItemRepository;

    @Transactional
    public List<StockRowDto> listByWarehouse(Long warehouseId) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        List<Stock> rows = stockRepository.findAllByWarehouse_IdAndMaster_Id(warehouseId, masterId);
        return rows.stream().map(s -> toDtoWithAgg(s, warehouseId, masterId)).collect(Collectors.toList());
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
}
