package kirillzhdanov.identityservice.controller.inventory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Нет доступа")
    })
    public ResponseEntity<List<UnitDto>> list() {
        rbacGuard.requireStaffOrHigher();
        return ResponseEntity.ok(unitService.list());
    }

    @PostMapping
    @Operation(summary = "Создать unit", description = "OWNER/ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Создано"),
            @ApiResponse(responseCode = "400", description = "Неверный запрос"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    public ResponseEntity<UnitDto> create(@RequestBody UnitDto dto) {
        rbacGuard.requireOwnerOrAdmin();
        return new ResponseEntity<>(unitService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить unit", description = "OWNER/ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Обновлено"),
            @ApiResponse(responseCode = "400", description = "Неверный запрос"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Не найдено")
    })
    public ResponseEntity<UnitDto> update(@PathVariable Long id, @RequestBody UnitDto dto) {
        rbacGuard.requireOwnerOrAdmin();
        return ResponseEntity.ok(unitService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить unit", description = "OWNER/ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Удалено"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Не найдено")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rbacGuard.requireOwnerOrAdmin();
        unitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
