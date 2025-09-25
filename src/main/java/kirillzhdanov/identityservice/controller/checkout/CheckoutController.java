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
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
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
@Slf4j
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @PostMapping
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest req, HttpServletRequest request) {
        Optional<User> userOpt = getCurrentUserWithDiagnostics(request);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Требуется авторизация"));
        }
        try {
            DeliveryMode mode = DeliveryMode.valueOf(req.getMode());
            String cartToken = request != null ? request.getHeader("X-Cart-Token") : null;
            Order order = checkoutService.createOrderFromCart(
                    userOpt.get(),
                    mode,
                    req.getAddressId(),
                    req.getPickupPointId(),
                    req.getComment(),
                    cartToken
            );
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
    public ResponseEntity<?> myOrders(HttpServletRequest request) {
        Optional<User> userOpt = getCurrentUserWithDiagnostics(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(401).build();
        List<Order> orders = orderRepository.findByClient_IdOrderByIdDesc(userOpt.get().getId());
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

    private Optional<User> getCurrentUserWithDiagnostics(HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String authz = request != null ? request.getHeader("Authorization") : null;
            if (auth == null) {
                log.warn("[checkout] Authentication is null, Authorization header present? {}", authz != null);
                return Optional.empty();
            }
            if (!auth.isAuthenticated()) {
                log.warn("[checkout] Authentication is not authenticated. Principal={} AuthzPresent={} Class={}",
                        auth.getPrincipal(), authz != null, auth.getClass().getSimpleName());
                return Optional.empty();
            }
            Object principal = auth.getPrincipal();
            String candidate = null;
            if (principal instanceof org.springframework.security.core.userdetails.User u) {
                candidate = u.getUsername();
            } else if (principal instanceof String s) {
                candidate = s;
            } else {
                candidate = auth.getName();
            }
            log.info("[checkout] Auth principalClass={}, name={}, authzPresent={}",
                    principal != null ? principal.getClass().getSimpleName() : "null",
                    candidate, authz != null);
            // Пытаемся по username
            if (candidate != null) {
                Optional<User> byUsername = userRepository.findByUsername(candidate);
                if (byUsername.isPresent()) return byUsername;
                // Пытаемся по email
                try {
                    Optional<User> byEmail = userRepository.findByEmail(candidate);
                    if (byEmail.isPresent()) return byEmail;
                } catch (Exception ignored) {
                }
                // Пытаемся по id, если строка — число
                try {
                    long id = Long.parseLong(candidate);
                    Optional<User> byId = userRepository.findById(id);
                    if (byId.isPresent()) return byId;
                } catch (NumberFormatException ignored) {
                }
            }
            log.warn("[checkout] User not resolved from principal='{}'", candidate);
            return Optional.empty();
        } catch (Exception e) {
            log.error("[checkout] Failed to resolve user: {}", e.toString());
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
