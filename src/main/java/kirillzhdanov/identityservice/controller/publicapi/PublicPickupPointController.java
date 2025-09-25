package kirillzhdanov.identityservice.controller.publicapi;

import kirillzhdanov.identityservice.model.pickup.PickupPoint;
import kirillzhdanov.identityservice.repository.pickup.PickupPointRepository;
import kirillzhdanov.identityservice.util.BrandContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/public/v1/pickup-points")
@RequiredArgsConstructor
public class PublicPickupPointController {

    private final PickupPointRepository pickupPointRepository;

    @GetMapping
    public ResponseEntity<List<PickupPoint>> listActiveForCurrentBrand() {
        Long brandId = BrandContextHolder.get();
        if (brandId == null) {
            return ResponseEntity.ok(List.of());
        }
        List<PickupPoint> list = pickupPointRepository.findByBrand_IdAndActiveTrue(brandId);
        return ResponseEntity.ok(list);
    }
}
