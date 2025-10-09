package kirillzhdanov.identityservice.controller.checkout;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.order.DeliveryMode;
import kirillzhdanov.identityservice.model.order.Order;
import kirillzhdanov.identityservice.model.order.OrderStatus;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.order.OrderRepository;
import kirillzhdanov.identityservice.service.CheckoutService;
import kirillzhdanov.identityservice.tenant.ContextAccess;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Контроллер оформления заказа из корзины.
 * <p>
 * Простой алгоритм:
 * 1) Определяем текущего пользователя.
 * 2) Читаем режим доставки: самовывоз (PICKUP) или доставка (DELIVERY).
 * 3) Читаем идентификатор корзины из HttpOnly cookie "cart_token".
 * 4) Определяем точку самовывоза: либо из тела запроса, либо из cookie‑контекста (ContextAccess).
 * 5) Создаём заказ и возвращаем его данные.
 */
@RestController
@RequestMapping("/checkout")
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    /**
     * Оформляет заказ на основании текущей корзины.
     * <p>
     * Пояснения:
     * - Корзина идентифицируется HttpOnly cookie "cart_token" (браузер отправляет автоматически).
     * - Для режима PICKUP точка самовывоза обязательна: можно передать в теле запроса или выбрать заранее
     * через установку контекста (см. /auth/v1/context), тогда берём из cookie‑контекста.
     */
    @PostMapping
    @Operation(summary = "Оформление заказа (из корзины)", description = "Требуется аутентификация. Создаёт заказ из текущей корзины пользователя.")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest req, HttpServletRequest request) {
        Optional<User> userOpt = getCurrentUserWithDiagnostics(request);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Требуется авторизация"));
        }
        try {
            DeliveryMode mode = DeliveryMode.valueOf(req.getMode());
            // Read cart_token from HttpOnly cookies (no headers)
            String cartToken = null;
            if (request != null && request.getCookies() != null) {
                for (Cookie c : request.getCookies()) {
                    if ("cart_token".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                        cartToken = c.getValue();
                        break;
                    }
                }
            }
            // Determine pickup point: explicit in body or from cookie-based context
            Long pickupId = req.getPickupPointId() != null ? req.getPickupPointId() : ContextAccess.getPickupPointIdOrNull();
            if (mode == DeliveryMode.PICKUP && pickupId == null) {
                return ResponseEntity.badRequest().body(Map.of("code", "PICKUP_POINT_REQUIRED", "message", "Не выбрана точка самовывоза"));
            }
            Order order = checkoutService.createOrderFromCart(
                    userOpt.get(),
                    mode,
                    req.getAddressId(),
                    pickupId,
                    req.getComment(),
                    cartToken
            );
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Не удалось оформить заказ", "error", e.getClass().getSimpleName()));
        }
    }

    // Заказы текущего пользователя
    @GetMapping("/my")
    @Operation(summary = "Мои заказы", description = "Требуется аутентификация. Возвращает список заказов текущего пользователя.")
    public ResponseEntity<?> myOrders(HttpServletRequest request) {
        Optional<User> userOpt = getCurrentUserWithDiagnostics(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(401).build();
        List<Order> orders = orderRepository.findByClient_IdOrderByIdDesc(userOpt.get().getId());
        return ResponseEntity.ok(orders);
    }

    // Простая смена статуса заказа (для демо). В продуктиве: добавить проверку прав бренда/персонала.
    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Смена статуса заказа (демо)", description = "Требуется аутентификация. В продуктиве: ограничить по ролям (CASHIER/COOK/ADMIN/OWNER) и бренду.")
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
            String candidate;
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
