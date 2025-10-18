package kirillzhdanov.identityservice.controller.inventory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.inventory.CreateIngredientRequest;
import kirillzhdanov.identityservice.dto.inventory.IngredientDto;
import kirillzhdanov.identityservice.dto.inventory.IngredientWithStockDto;
import kirillzhdanov.identityservice.dto.inventory.UpdateIngredientRequest;
import kirillzhdanov.identityservice.security.RbacGuard;
import kirillzhdanov.identityservice.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/v1/inventory/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;
    private final RbacGuard rbacGuard;

    @GetMapping(params = "allWarehouses")
    @Operation(summary = "Ингредиенты с остатками по всем складам", description = "AUTH. Отдаёт строки остатков по всем складам текущего master: по одной записи на склад.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Нет доступа")
    })
    public ResponseEntity<List<IngredientWithStockDto>> listWithStockAll(@RequestParam("allWarehouses") boolean all) {
        rbacGuard.requireStaffOrHigher();
        if (!all) {
            return ResponseEntity.ok(java.util.List.of());
        }
        return ResponseEntity.ok(ingredientService.listWithStockAll());
    }

    @GetMapping(params = "warehouseId")
    @Operation(summary = "Ингредиенты с остатками по складу", description = "AUTH. Возвращает ингредиенты с количеством по указанному складу в текущем master контексте.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Нет доступа")
    })
    public ResponseEntity<List<IngredientWithStockDto>> listWithStock(@RequestParam("warehouseId") Long warehouseId) {
        rbacGuard.requireStaffOrHigher();
        return ResponseEntity.ok(ingredientService.listWithStock(warehouseId));
    }

    @GetMapping
    @Operation(summary = "Список ингредиентов", description = "AUTH. Возвращает ингредиенты текущего master контекста.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Нет доступа")
    })
    public ResponseEntity<List<IngredientDto>> list() {
        rbacGuard.requireStaffOrHigher();
        return ResponseEntity.ok(ingredientService.list());
    }

    @PostMapping
    @Operation(summary = "Создать ингредиент", description = "OWNER/ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Создано"),
            @ApiResponse(responseCode = "400", description = "Неверный запрос"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    public ResponseEntity<IngredientDto> create(@RequestBody @Valid CreateIngredientRequest req) {
        rbacGuard.requireOwnerOrAdmin();
        return new ResponseEntity<>(ingredientService.create(req), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить ингредиент", description = "OWNER/ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Обновлено"),
            @ApiResponse(responseCode = "400", description = "Неверный запрос"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Не найдено")
    })
    public ResponseEntity<IngredientDto> update(@PathVariable Long id, @RequestBody @Valid UpdateIngredientRequest req) {
        rbacGuard.requireOwnerOrAdmin();
        return ResponseEntity.ok(ingredientService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить ингредиент", description = "OWNER/ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Удалено"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Не найдено")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rbacGuard.requireOwnerOrAdmin();
        ingredientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
