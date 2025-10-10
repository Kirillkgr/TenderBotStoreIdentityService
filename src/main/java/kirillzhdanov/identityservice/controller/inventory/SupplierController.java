package kirillzhdanov.identityservice.controller.inventory;

import io.swagger.v3.oas.annotations.Operation;
import kirillzhdanov.identityservice.dto.inventory.SupplierDto;
import kirillzhdanov.identityservice.security.RbacGuard;
import kirillzhdanov.identityservice.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/v1/inventory/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;
    private final RbacGuard rbacGuard;

    @GetMapping
    @Operation(summary = "Список поставщиков", description = "AUTH. Возвращает поставщиков текущего master контекста.")
    public ResponseEntity<List<SupplierDto>> list() {
        rbacGuard.requireAuthenticated();
        return ResponseEntity.ok(supplierService.list());
    }

    @PostMapping
    @Operation(summary = "Создать поставщика", description = "OWNER/ADMIN")
    public ResponseEntity<SupplierDto> create(@RequestBody SupplierDto dto) {
        rbacGuard.requireOwnerOrAdmin();
        return new ResponseEntity<>(supplierService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить поставщика", description = "OWNER/ADMIN")
    public ResponseEntity<SupplierDto> update(@PathVariable Long id, @RequestBody SupplierDto dto) {
        rbacGuard.requireOwnerOrAdmin();
        return ResponseEntity.ok(supplierService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить поставщика", description = "OWNER/ADMIN")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rbacGuard.requireOwnerOrAdmin();
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
