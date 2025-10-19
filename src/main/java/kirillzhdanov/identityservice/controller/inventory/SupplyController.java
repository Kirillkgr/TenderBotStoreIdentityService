package kirillzhdanov.identityservice.controller.inventory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.inventory.supply.CreateSupplyRequest;
import kirillzhdanov.identityservice.dto.inventory.supply.SupplyDto;
import kirillzhdanov.identityservice.dto.inventory.supply.UpdateSupplyRequest;
import kirillzhdanov.identityservice.model.inventory.Supply;
import kirillzhdanov.identityservice.security.RbacGuard;
import kirillzhdanov.identityservice.service.SupplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Создано"),
            @ApiResponse(responseCode = "400", description = "Неверный запрос", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизован", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
    })
    public ResponseEntity<?> create(@Valid @RequestBody CreateSupplyRequest req) {
        rbacGuard.requireOwnerOrAdmin();
        Supply s = supplyService.create(req);
        return ResponseEntity.created(URI.create("/auth/v1/inventory/supplies/" + s.getId()))
                .body(java.util.Map.of("id", s.getId(), "status", s.getStatus()));
    }

    @PostMapping("/search")
    @Operation(summary = "Поиск/листинг поставок (POST for filters/pagination)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Неавторизован", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Нет доступа", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
    })
    public ResponseEntity<Page<SupplyDto>> search(@Parameter(description = "ID склада") @RequestParam(required = false) Long warehouseId,
                                                  @Parameter(description = "Статус поставки") @RequestParam(required = false) String status,
                                                  @Parameter(description = "Номер страницы (0..)") @RequestParam(defaultValue = "0") int page,
                                                  @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "20") int size) {
        rbacGuard.requireStaffOrHigher();
        Page<SupplyDto> res = supplyService.search(warehouseId, status, PageRequest.of(Math.max(0, page), Math.max(1, size)));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить поставку по id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Неавторизован", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Нет доступа", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Не найдено", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
    })
    public ResponseEntity<SupplyDto> get(@Parameter(description = "ID поставки") @PathVariable Long id) {
        rbacGuard.requireStaffOrHigher();
        Supply s = supplyService.get(id);
        return ResponseEntity.ok(supplyService.toDto(s));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить поставку (только DRAFT)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Обновлено"),
            @ApiResponse(responseCode = "400", description = "Неверный запрос", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизован", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Не найдено", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
    })
    public ResponseEntity<SupplyDto> update(@Parameter(description = "ID поставки") @PathVariable Long id, @Valid @RequestBody UpdateSupplyRequest req) {
        rbacGuard.requireOwnerOrAdmin();
        Supply s = supplyService.update(id, req);
        return ResponseEntity.ok(supplyService.toDto(s));
    }

    @PostMapping("/{id}/post")
    @Operation(summary = "Провести поставку")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Проведено"),
            @ApiResponse(responseCode = "400", description = "Неверный запрос", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизован", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Не найдено", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
    })
    public ResponseEntity<?> post(@Parameter(description = "ID поставки") @PathVariable Long id) {
        rbacGuard.requireOwnerOrAdmin();
        Supply s = supplyService.post(id);
        return ResponseEntity.ok(java.util.Map.of("id", s.getId(), "status", s.getStatus()));
    }
}
