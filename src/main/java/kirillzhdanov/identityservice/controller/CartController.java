package kirillzhdanov.identityservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cart")
public class CartController {

    @GetMapping
    public ResponseEntity<Void> getCart() {
        // Заглушка: ничего не возвращаем, чтобы фронт не зацикливался.
        // В дальнейшем: если аутентифицирован, вернуть содержимое корзины.
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
