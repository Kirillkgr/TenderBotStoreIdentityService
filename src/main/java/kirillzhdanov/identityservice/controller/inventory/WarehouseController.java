package kirillzhdanov.identityservice.controller.inventory;

import io.swagger.v3.oas.annotations.Operation;
import kirillzhdanov.identityservice.dto.inventory.WarehouseDto;
import kirillzhdanov.identityservice.security.RbacGuard;
import kirillzhdanov.identityservice.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/v1/inventory/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final RbacGuard rbacGuard;

    @GetMapping
    @Operation(summary = "Список складов", description = "Требуется аутентификация. Возвращает склады текущего master контекста.")
    public ResponseEntity<List<WarehouseDto>> list() {
        rbacGuard.requireAuthenticated();
        return ResponseEntity.ok(warehouseService.list());
    }

    @PostMapping
    @Operation(summary = "Создать склад", description = "Требуется роль OWNER или ADMIN.")
    public ResponseEntity<WarehouseDto> create(@RequestBody WarehouseDto dto) {
        rbacGuard.requireOwnerOrAdmin();
        return new ResponseEntity<>(warehouseService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить склад", description = "Требуется роль OWNER или ADMIN. Доступ только к своему master.")
    public ResponseEntity<WarehouseDto> update(@PathVariable Long id, @RequestBody WarehouseDto dto) {
        rbacGuard.requireOwnerOrAdmin();
        return ResponseEntity.ok(warehouseService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить склад", description = "Требуется роль OWNER или ADMIN. Доступ только к своему master.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rbacGuard.requireOwnerOrAdmin();
        warehouseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
