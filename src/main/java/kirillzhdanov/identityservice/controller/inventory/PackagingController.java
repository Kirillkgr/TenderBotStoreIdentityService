package kirillzhdanov.identityservice.controller.inventory;

import io.swagger.v3.oas.annotations.Operation;
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
public class PackagingController {

    private final PackagingService packagingService;
    private final RbacGuard rbacGuard;

    @GetMapping
    @Operation(summary = "Список фасовок")
    public ResponseEntity<List<PackagingDto>> list() {
        rbacGuard.requireAuthenticated();
        return ResponseEntity.ok(packagingService.list());
    }

    @PostMapping
    @Operation(summary = "Создать фасовку")
    public ResponseEntity<PackagingDto> create(@Valid @RequestBody PackagingDto req) {
        rbacGuard.requireOwnerOrAdmin();
        PackagingDto created = packagingService.create(req);
        return ResponseEntity.created(URI.create("/auth/v1/inventory/packagings/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить фасовку")
    public ResponseEntity<PackagingDto> update(@PathVariable Long id, @Valid @RequestBody PackagingDto req) {
        rbacGuard.requireOwnerOrAdmin();
        return ResponseEntity.ok(packagingService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить фасовку")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rbacGuard.requireOwnerOrAdmin();
        packagingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
