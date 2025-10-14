package kirillzhdanov.identityservice.controller.inventory;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.inventory.supply.CreateSupplyRequest;
import kirillzhdanov.identityservice.dto.inventory.supply.UpdateSupplyRequest;
import kirillzhdanov.identityservice.dto.inventory.supply.SupplyDto;
import kirillzhdanov.identityservice.model.inventory.Supply;
import kirillzhdanov.identityservice.security.RbacGuard;
import kirillzhdanov.identityservice.service.SupplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

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

    @PostMapping("/search")
    @Operation(summary = "Поиск/листинг поставок (POST for filters/pagination)")
    public ResponseEntity<Page<SupplyDto>> search(@RequestParam(required = false) Long warehouseId,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        rbacGuard.requireOwnerOrAdmin();
        Page<SupplyDto> res = supplyService.search(warehouseId, status, PageRequest.of(Math.max(0, page), Math.max(1, size)));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить поставку по id")
    public ResponseEntity<SupplyDto> get(@PathVariable Long id) {
        rbacGuard.requireOwnerOrAdmin();
        Supply s = supplyService.get(id);
        return ResponseEntity.ok(supplyService.toDto(s));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить поставку (только DRAFT)")
    public ResponseEntity<SupplyDto> update(@PathVariable Long id, @Valid @RequestBody UpdateSupplyRequest req) {
        rbacGuard.requireOwnerOrAdmin();
        Supply s = supplyService.update(id, req);
        return ResponseEntity.ok(supplyService.toDto(s));
    }

    @PostMapping("/{id}/post")
    @Operation(summary = "Провести поставку")
    public ResponseEntity<?> post(@PathVariable Long id) {
        rbacGuard.requireOwnerOrAdmin();
        Supply s = supplyService.post(id);
        return ResponseEntity.ok(java.util.Map.of("id", s.getId(), "status", s.getStatus()));
    }
}
