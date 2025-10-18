package kirillzhdanov.identityservice.controller.inventory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.inventory.packaging.PackagingDto;
import kirillzhdanov.identityservice.security.RbacGuard;
import kirillzhdanov.identityservice.service.PackagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/auth/v1/inventory/packagings")
@RequiredArgsConstructor
@Tag(name = "Inventory: Packaging")
public class PackagingController {

    private final PackagingService packagingService;
    private final RbacGuard rbacGuard;

    @GetMapping
    @Operation(summary = "Список фасовок")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Нет доступа")
    })
    public ResponseEntity<List<PackagingDto>> list() {
        rbacGuard.requireStaffOrHigher();
        return ResponseEntity.ok(packagingService.list());
    }

    @PostMapping
    @Operation(summary = "Создать фасовку")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Создано"),
        @ApiResponse(responseCode = "400", description = "Неверный запрос"),
        @ApiResponse(responseCode = "401", description = "Неавторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    public ResponseEntity<PackagingDto> create(@Valid @RequestBody PackagingDto req) {
        rbacGuard.requireOwnerOrAdmin();
        PackagingDto created = packagingService.create(req);
        return ResponseEntity.created(URI.create("/auth/v1/inventory/packagings/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить фасовку")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Обновлено"),
        @ApiResponse(responseCode = "400", description = "Неверный запрос"),
        @ApiResponse(responseCode = "401", description = "Неавторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        @ApiResponse(responseCode = "404", description = "Не найдено")
    })
    public ResponseEntity<PackagingDto> update(@PathVariable Long id, @Valid @RequestBody PackagingDto req) {
        rbacGuard.requireOwnerOrAdmin();
        return ResponseEntity.ok(packagingService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить фасовку")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Удалено"),
        @ApiResponse(responseCode = "401", description = "Неавторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        @ApiResponse(responseCode = "404", description = "Не найдено")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rbacGuard.requireOwnerOrAdmin();
        packagingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
