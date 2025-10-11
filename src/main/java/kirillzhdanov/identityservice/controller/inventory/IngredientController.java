package kirillzhdanov.identityservice.controller.inventory;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.inventory.CreateIngredientRequest;
import kirillzhdanov.identityservice.dto.inventory.IngredientDto;
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

    @GetMapping
    @Operation(summary = "Список ингредиентов", description = "AUTH. Возвращает ингредиенты текущего master контекста.")
    public ResponseEntity<List<IngredientDto>> list() {
        rbacGuard.requireAuthenticated();
        return ResponseEntity.ok(ingredientService.list());

    }

    @PostMapping
    @Operation(summary = "Создать ингредиент", description = "OWNER/ADMIN")
    public ResponseEntity<IngredientDto> create(@RequestBody @Valid CreateIngredientRequest req) {
        rbacGuard.requireOwnerOrAdmin();
        return new ResponseEntity<>(ingredientService.create(req), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить ингредиент", description = "OWNER/ADMIN")
    public ResponseEntity<IngredientDto> update(@PathVariable Long id, @RequestBody @Valid UpdateIngredientRequest req) {
        rbacGuard.requireOwnerOrAdmin();
        return ResponseEntity.ok(ingredientService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить ингредиент", description = "OWNER/ADMIN")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rbacGuard.requireOwnerOrAdmin();
        ingredientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
