package kirillzhdanov.identityservice.controller.inventory;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.inventory.supply.CreateSupplyRequest;
import kirillzhdanov.identityservice.model.inventory.Supply;
import kirillzhdanov.identityservice.security.RbacGuard;
import kirillzhdanov.identityservice.service.SupplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/auth/v1/inventory/supplies")
@RequiredArgsConstructor
public class SupplyController {

    private final SupplyService supplyService;
    private final RbacGuard rbacGuard;

    @PostMapping
    @Operation(summary = "Создать черновик поставки")
    public ResponseEntity<?> create(@Valid @RequestBody CreateSupplyRequest req) {
        rbacGuard.requireOwnerOrAdmin();
        Supply s = supplyService.create(req);
        return ResponseEntity.created(URI.create("/auth/v1/inventory/supplies/" + s.getId()))
                .body(java.util.Map.of("id", s.getId(), "status", s.getStatus()));
    }

    @PostMapping("/{id}/post")
    @Operation(summary = "Провести поставку")
    public ResponseEntity<?> post(@PathVariable Long id) {
        rbacGuard.requireOwnerOrAdmin();
        Supply s = supplyService.post(id);
        return ResponseEntity.ok(java.util.Map.of("id", s.getId(), "status", s.getStatus()));
    }
}
