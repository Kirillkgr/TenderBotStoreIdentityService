package kirillzhdanov.identityservice.controller.inventory;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotNull;
import kirillzhdanov.identityservice.dto.inventory.StockRowDto;
import kirillzhdanov.identityservice.security.RbacGuard;
import kirillzhdanov.identityservice.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth/v1/inventory/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final RbacGuard rbacGuard;

    @GetMapping
    @Operation(summary = "Список остатков по складу", description = "Требуется аутентификация. Роли: ADMIN/OWNER/COOK/CASHIER")
    public ResponseEntity<List<StockRowDto>> listByWarehouse(@RequestParam("warehouseId") @NotNull Long warehouseId) {
        rbacGuard.requireAuthenticated(); // чтение разрешено аутентифицированным (COOK/CASHIER в том числе)
        return ResponseEntity.ok(stockService.listByWarehouse(warehouseId));
    }
}
