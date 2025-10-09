package kirillzhdanov.identityservice.controller.publicapi;

import io.swagger.v3.oas.annotations.Operation;
import kirillzhdanov.identityservice.model.pickup.PickupPoint;
import kirillzhdanov.identityservice.repository.pickup.PickupPointRepository;
import kirillzhdanov.identityservice.tenant.ContextAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Публичный контроллер списков пунктов самовывоза текущего бренда.
 * Источник бренда — контекст из HttpOnly cookie (через ContextAccess).
 */
@RestController
@RequestMapping("/public/v1/pickup-points")
@RequiredArgsConstructor
public class PublicPickupPointController {

    private final PickupPointRepository pickupPointRepository;

    /**
     * Возвращает активные пункты самовывоза текущего бренда (публично).
     * Если бренд не определён в контексте — возвращает пустой список.
     */
    @GetMapping
    @Operation(summary = "Публичные пункты самовывоза текущего бренда", description = "Публично. Определяет brandId из контекста и возвращает активные точки.")
    public ResponseEntity<List<PickupPoint>> listActiveForCurrentBrand() {
        Long brandId = ContextAccess.getBrandIdOrNull();
        if (brandId == null) {
            return ResponseEntity.ok(List.of());
        }
        List<PickupPoint> list = pickupPointRepository.findByBrand_IdAndActiveTrue(brandId);
        return ResponseEntity.ok(list);
    }
}
