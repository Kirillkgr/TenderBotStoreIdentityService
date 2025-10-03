package kirillzhdanov.identityservice.controller;

import kirillzhdanov.identityservice.security.RbacGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kitchen/v1")
@RequiredArgsConstructor
public class KitchenController {

    private final RbacGuard rbacGuard;

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        // Allow only COOK role
        rbacGuard.requireCook();
        return ResponseEntity.ok("OK");
    }
}
