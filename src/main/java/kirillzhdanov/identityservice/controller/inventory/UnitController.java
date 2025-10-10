package kirillzhdanov.identityservice.controller.inventory;

import io.swagger.v3.oas.annotations.Operation;
import kirillzhdanov.identityservice.dto.inventory.UnitDto;
import kirillzhdanov.identityservice.security.RbacGuard;
import kirillzhdanov.identityservice.service.UnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/v1/inventory/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;
    private final RbacGuard rbacGuard;

    @GetMapping
    @Operation(summary = "Список единиц измерения", description = "AUTH. Возвращает units текущего master контекста.")
    public ResponseEntity<List<UnitDto>> list() {
        rbacGuard.requireAuthenticated();
        return ResponseEntity.ok(unitService.list());
    }

    @PostMapping
    @Operation(summary = "Создать unit", description = "OWNER/ADMIN")
    public ResponseEntity<UnitDto> create(@RequestBody UnitDto dto) {
        rbacGuard.requireOwnerOrAdmin();
        return new ResponseEntity<>(unitService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить unit", description = "OWNER/ADMIN")
    public ResponseEntity<UnitDto> update(@PathVariable Long id, @RequestBody UnitDto dto) {
        rbacGuard.requireOwnerOrAdmin();
        return ResponseEntity.ok(unitService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить unit", description = "OWNER/ADMIN")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rbacGuard.requireOwnerOrAdmin();
        unitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
