package kirillzhdanov.identityservice.controller.pickup;

import kirillzhdanov.identityservice.model.pickup.PickupPoint;
import kirillzhdanov.identityservice.repository.pickup.PickupPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/brand/{brandId}/pickup-points")
@RequiredArgsConstructor
public class PickupPointController {

    private final PickupPointRepository pickupPointRepository;

    @GetMapping
    public ResponseEntity<List<PickupPoint>> getActivePickupPoints(@PathVariable Long brandId,
                                                                   Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).build();
        }
        List<PickupPoint> list = pickupPointRepository.findByBrand_IdAndActiveTrue(brandId);
        return ResponseEntity.ok(list);
    }
}
