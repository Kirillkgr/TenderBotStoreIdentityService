package kirillzhdanov.identityservice.controller.inventory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.inventory.StockAdjustRequest;
import kirillzhdanov.identityservice.dto.inventory.StockRowDto;
import kirillzhdanov.identityservice.security.RbacGuard;
import kirillzhdanov.identityservice.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/v1/inventory/stock")
@RequiredArgsConstructor
@Tag(name = "Inventory: Stock")
public class StockController {

    private final StockService stockService;
    private final RbacGuard rbacGuard;

    @GetMapping
    @Operation(summary = "Список остатков (фильтры)", description = "Требуется аутентификация. Роли: ADMIN/OWNER/COOK/CASHIER")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "400", description = "Неверные параметры (нет фильтров)"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Нет доступа")
    })
    public ResponseEntity<List<StockRowDto>> list(
            @RequestParam(value = "ingredientId", required = false) Long ingredientId,
            @RequestParam(value = "warehouseId", required = false) Long warehouseId) {
        rbacGuard.requireStaffOrHigher(); // чтение разрешено персоналу (COOK/CASHIER/ADMIN/OWNER)
        if (ingredientId == null && warehouseId != null) {
            return ResponseEntity.ok(stockService.listByWarehouse(warehouseId));
        }
        return ResponseEntity.ok(stockService.list(ingredientId, warehouseId));
    }

    @PostMapping("/increase")
    @Operation(summary = "Увеличить остаток (приход)", description = "RBAC: OWNER/ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Остаток увеличен"),
            @ApiResponse(responseCode = "400", description = "Неверный запрос (qty<0, несуществующие сущности)"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    public ResponseEntity<StockRowDto> increase(@Valid @RequestBody StockAdjustRequest request) {
        rbacGuard.requireOwnerOrAdmin();
        return ResponseEntity.ok(stockService.increaseStock(request));
    }

    @PostMapping("/decrease")
    @Operation(summary = "Уменьшить остаток (списание)", description = "RBAC: OWNER/ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Остаток уменьшен"),
            @ApiResponse(responseCode = "400", description = "Недостаточно остатка или неверный запрос"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    public ResponseEntity<StockRowDto> decrease(@Valid @RequestBody StockAdjustRequest request) {
        rbacGuard.requireOwnerOrAdmin();
        return ResponseEntity.ok(stockService.decreaseStock(request));
    }
}
