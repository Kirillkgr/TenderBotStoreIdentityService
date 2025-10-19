package kirillzhdanov.identityservice.controller.inventory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.inventory.CreateWarehouseRequest;
import kirillzhdanov.identityservice.dto.inventory.UpdateWarehouseRequest;
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
@Tag(name = "Inventory: Warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final RbacGuard rbacGuard;

    @GetMapping
    @Operation(summary = "Список складов", description = "Требуется аутентификация. Возвращает склады текущего master контекста.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Неавторизован", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Нет доступа", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
    })
    public ResponseEntity<List<WarehouseDto>> list() {
        rbacGuard.requireStaffOrHigher();
        return ResponseEntity.ok(warehouseService.list());
    }

    @PostMapping
    @Operation(summary = "Создать склад", description = "Требуется роль OWNER или ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Создано"),
            @ApiResponse(responseCode = "400", description = "Неверный запрос", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизован", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "Конфликт (дубликат)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
    })
    public ResponseEntity<WarehouseDto> create(@Valid @RequestBody CreateWarehouseRequest req) {
        rbacGuard.requireOwnerOrAdmin();
        WarehouseDto dto = new WarehouseDto(null, req.getName());
        return new ResponseEntity<>(warehouseService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить склад", description = "Требуется роль OWNER или ADMIN. Доступ только к своему master.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Обновлено"),
            @ApiResponse(responseCode = "400", description = "Неверный запрос", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизован", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Не найдено", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
    })
    public ResponseEntity<WarehouseDto> update(@Parameter(description = "ID склада") @PathVariable Long id, @Valid @RequestBody UpdateWarehouseRequest req) {
        rbacGuard.requireOwnerOrAdmin();
        WarehouseDto dto = new WarehouseDto(id, req.getName());
        return ResponseEntity.ok(warehouseService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить склад", description = "Требуется роль OWNER или ADMIN. Доступ только к своему master.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Удалено"),
            @ApiResponse(responseCode = "401", description = "Неавторизован", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Не найдено", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
    })
    public ResponseEntity<Void> delete(@Parameter(description = "ID склада") @PathVariable Long id) {
        rbacGuard.requireOwnerOrAdmin();
        warehouseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
