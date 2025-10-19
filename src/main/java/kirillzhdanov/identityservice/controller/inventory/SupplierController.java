package kirillzhdanov.identityservice.controller.inventory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Inventory: Suppliers")
public class SupplierController {

    private final SupplierService supplierService;
    private final RbacGuard rbacGuard;

    @GetMapping
    @Operation(summary = "Список поставщиков", description = "AUTH. Возвращает поставщиков текущего master контекста.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Неавторизован", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Нет доступа", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
    })
    public ResponseEntity<List<SupplierDto>> list() {
        rbacGuard.requireStaffOrHigher();
        return ResponseEntity.ok(supplierService.list());
    }

    @PostMapping
    @Operation(summary = "Создать поставщика", description = "OWNER/ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Создано"),
            @ApiResponse(responseCode = "400", description = "Неверный запрос", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизован", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "Конфликт (дубликат)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
    })
    public ResponseEntity<SupplierDto> create(@RequestBody SupplierDto dto) {
        rbacGuard.requireOwnerOrAdmin();
        return new ResponseEntity<>(supplierService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить поставщика", description = "OWNER/ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Обновлено"),
            @ApiResponse(responseCode = "400", description = "Неверный запрос", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизован", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Не найдено", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "Конфликт", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
    })
    public ResponseEntity<SupplierDto> update(@Parameter(description = "ID поставщика") @PathVariable Long id, @RequestBody SupplierDto dto) {
        rbacGuard.requireOwnerOrAdmin();
        return ResponseEntity.ok(supplierService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить поставщика", description = "OWNER/ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Удалено"),
            @ApiResponse(responseCode = "401", description = "Неавторизован", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Не найдено", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
    })
    public ResponseEntity<Void> delete(@Parameter(description = "ID поставщика") @PathVariable Long id) {
        rbacGuard.requireOwnerOrAdmin();
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
