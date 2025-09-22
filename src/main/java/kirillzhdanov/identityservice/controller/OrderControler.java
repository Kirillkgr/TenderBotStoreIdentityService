package kirillzhdanov.identityservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderControler {

    @GetMapping("/orders")
    public ResponseEntity<Void> ordersStub() {
        // Stub endpoint to avoid long waits on the client side
        return ResponseEntity.noContent().build();
    }
}
