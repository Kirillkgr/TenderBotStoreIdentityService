package kirillzhdanov.identityservice.controller.checkout;

import jakarta.validation.constraints.NotNull;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.order.DeliveryMode;
import kirillzhdanov.identityservice.model.order.Order;
import kirillzhdanov.identityservice.model.order.OrderStatus;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.order.OrderRepository;
import kirillzhdanov.identityservice.service.CheckoutService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @PostMapping
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest req) {
        Optional<User> userOpt = getCurrentUser();
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Требуется авторизация"));
        }
        try {
            DeliveryMode mode = DeliveryMode.valueOf(req.getMode());
            Order order = checkoutService.createOrderFromCart(userOpt.get(), mode, req.getAddressId(), req.getPickupPointId(), req.getComment());
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Не удалось оформить заказ", "error", e.getClass().getSimpleName()));
        }
    }

    // Заказы текущего пользователя
    @GetMapping("/my")
    public ResponseEntity<?> myOrders() {
        Optional<User> userOpt = getCurrentUser();
        if (userOpt.isEmpty()) return ResponseEntity.status(401).build();
        List<Order> orders = orderRepository.findAll() // TODO: заменить на метод findByClient_Id при наличии
                .stream().filter(o -> o.getClient() != null && o.getClient().getId().equals(userOpt.get().getId()))
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .toList();
        return ResponseEntity.ok(orders);
    }

    // Простая смена статуса заказа (для демо). В продуктиве: добавить проверку прав бренда/персонала.
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<?> changeStatus(@PathVariable Long orderId, @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null) return ResponseEntity.badRequest().body(Map.of("message", "status обязателен"));
        Optional<Order> opt = orderRepository.findById(orderId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        try {
            OrderStatus st = OrderStatus.valueOf(statusStr);
            Order o = opt.get();
            o.setStatus(st);
            orderRepository.save(o);
            return ResponseEntity.ok(o);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Некорректный статус"));
        }
    }

    private Optional<User> getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return Optional.empty();
            String username = String.valueOf(auth.getPrincipal());
            if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User u) {
                username = u.getUsername();
            }
            return userRepository.findByUsername(username);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Data
    public static class CheckoutRequest {
        @NotNull
        private String mode; // DELIVERY | PICKUP
        private Long addressId;
        private Long pickupPointId;
        private String comment;
    }
}
