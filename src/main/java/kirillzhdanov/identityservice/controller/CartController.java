package kirillzhdanov.identityservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.cart.CartItem;
import kirillzhdanov.identityservice.model.product.Product;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.ProductRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.cart.CartItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * Контроллер корзины.
 * <p>
 * Ключевые моменты:
 * - Для гостя корзина определяется HttpOnly cookie "cart_token" (браузер отправляет автоматически).
 * - Для авторизованного пользователя корзина хранится по его идентификатору.
 * - Идентификаторы корзины не возвращаются на фронт (безопасность), вместо этого фронт всегда спрашивает
 * актуальное состояние через GET.
 */
@RestController
@RequestMapping("/cart")
@Slf4j
public class CartController {

    private final CartItemRepository cartRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;
    private final BrandRepository brandRepo;

    public CartController(CartItemRepository cartRepo, ProductRepository productRepo, UserRepository userRepo, BrandRepository brandRepo) {
        this.cartRepo = cartRepo;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
        this.brandRepo = brandRepo;
    }

    /**
     * Возвращает текущее состояние корзины.
     * Для гостя используется cookie "cart_token" (создаётся автоматически, если ещё нет).
     */
    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Текущая корзина", description = "Публично (гость/пользователь). Возвращает корзину по userId или по cart_token (cookie).")
    public ResponseEntity<?> getCart(HttpServletRequest request, HttpServletResponse response) {
        try {
            Optional<User> userOpt = getCurrentUser();
            String cartToken = getOrCreateCartToken(request, response);

            // Do not validate identifiers on GET; only return canonical identifiers back to the client

            List<CartItem> items = userOpt.map(u -> cartRepo.findByUser_Id(u.getId()))
                    .orElseGet(() -> cartRepo.findByCartToken(cartToken));

            Map<String, Object> body = toCartResponse(items);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("/cart GET failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "code", "CART_FETCH_ERROR",
                    "message", "Не удалось получить корзину"
            ));
        }
    }

    /**
     * Добавляет товар в корзину. Если корзина гостевая — создаёт/продлевает cookie "cart_token".
     * При попытке добавить товар из другого бренда возвращает 409 (конфликт бренда).
     */
    @PostMapping("/add")
    @Transactional
    @Operation(summary = "Добавить в корзину", description = "Публично (гость/пользователь). Создаёт/использует cart_token для гостя. Возвращает обновлённую корзину или 409 при конфликте бренда.")
    public ResponseEntity<?> addToCart(@RequestBody Map<String, Object> payload,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        try {
            Long productId = payload.get("productId") == null ? null : Long.valueOf(String.valueOf(payload.get("productId")));
            int quantity = payload.get("quantity") == null ? 1 : Integer.parseInt(String.valueOf(payload.get("quantity")));
            if (productId == null || quantity <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "productId и quantity обязательны"));
            }

            Optional<Product> productOpt = productRepo.findById(productId);
            if (productOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Товар не найден"));
            }
            Product product = productOpt.get();

            // Определим бренд товара (если в сущности нет - попробуем из контекста)
            Brand productBrand = product.getBrand();
            if (productBrand == null) {
                Long ctxBrandId = kirillzhdanov.identityservice.tenant.ContextAccess.getBrandIdOrNull();
                if (ctxBrandId != null) {
                    productBrand = brandRepo.findById(ctxBrandId).orElse(null);
                }
            }

            Long newBrandId = productBrand != null ? productBrand.getId() : null;

            Optional<User> userOpt = getCurrentUser();
            String cartToken = getOrCreateCartToken(request, response);

            List<CartItem> current = userOpt.map(u -> cartRepo.findByUser_Id(u.getId()))
                    .orElseGet(() -> cartRepo.findByCartToken(cartToken));

            // Проверка на конфликт бренда
            Long existingBrandId = current.stream().findFirst().map(ci -> ci.getBrand() != null ? ci.getBrand().getId() : null).orElse(null);
            if (existingBrandId != null && newBrandId != null && !existingBrandId.equals(newBrandId)) {
                // Возвращаем 409 без удаления — фронт сам решит, очищать ли корзину
                Map<String, Object> conflict = new HashMap<>();
                conflict.put("code", "CART_BRAND_CONFLICT");
                conflict.put("message", "Корзина содержит товары другого бренда");
                conflict.put("cleared", false);
                conflict.put("previousBrandId", existingBrandId);
                return ResponseEntity.status(409).body(conflict);
            }

            // Добавление/увеличение количества
            CartItem target = current.stream().filter(ci -> ci.getProduct() != null && Objects.equals(ci.getProduct().getId(), product.getId()))
                    .findFirst().orElse(null);
            if (target == null) {
                target = CartItem.builder()
                        .user(userOpt.orElse(null))
                        .cartToken(userOpt.isPresent() ? null : cartToken)
                        .brand(productBrand)
                        .product(product)
                        .quantity(quantity)
                        .build();
            } else {
                target.setQuantity((target.getQuantity() == null ? 0 : target.getQuantity()) + quantity);
            }
            cartRepo.save(target);

            // Вернём актуальную корзину
            List<CartItem> updated = userOpt.map(u -> cartRepo.findByUser_Id(u.getId()))
                    .orElseGet(() -> cartRepo.findByCartToken(cartToken));
            Map<String, Object> resp = toCartResponse(updated);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("/cart/add failed", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Не удалось добавить товар", "error", e.getClass().getSimpleName()));
        }
    }

    /**
     * Удаляет конкретную позицию корзины. Разрешено владельцу userId (для авторизованного)
     * или владельцу соответствующей guest‑корзины (проверка по cart_token).
     */
    @DeleteMapping("/remove/{id}")
    @Transactional
    @Operation(summary = "Удалить позицию из корзины", description = "Публично (гость/пользователь). Разрешено владельцу userId или владельцу cart_token.")
    public ResponseEntity<?> removeFromCart(@PathVariable("id") Long cartItemId,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        Optional<User> userOpt = getCurrentUser();
        String cartToken = getOrCreateCartToken(request, response);

        Optional<CartItem> found = cartRepo.findById(cartItemId);
        if (found.isEmpty()) return ResponseEntity.noContent().build();

        CartItem ci = found.get();
        // Явная проверка гостевой корзины: если пользователь гость и токен не совпадает — запрет
        if (userOpt.isEmpty() && ci.getUser() == null && !Objects.equals(cartToken, ci.getCartToken())) {
            return ResponseEntity.status(403).body(Map.of("message", "Недостаточно прав для удаления элемента корзины"));
        }
        boolean allowed = (userOpt.isPresent() && ci.getUser() != null && Objects.equals(ci.getUser().getId(), userOpt.get().getId()))
                || (userOpt.isEmpty() && Objects.equals(cartToken, ci.getCartToken()));
        if (!allowed) {
            return ResponseEntity.status(403).body(Map.of("message", "Недостаточно прав для удаления элемента корзины"));
        }
        cartRepo.delete(ci);

        // Вернём актуальную корзину
        List<CartItem> updated = userOpt.map(u -> cartRepo.findByUser_Id(u.getId()))
                .orElseGet(() -> cartRepo.findByCartToken(cartToken));
        if (updated.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(toCartResponse(updated));
    }

    /**
     * Меняет количество позиции. При qty <= 0 позиция удаляется.
     * Правила доступа те же: владелец userId или владелец guest‑корзины.
     */
    @PatchMapping("/item/{id}")
    @Transactional
    @Operation(summary = "Изменить количество позиции", description = "Публично (гость/пользователь). Разрешено владельцу userId или владельцу cart_token. qty<=0 удаляет позицию.")
    public ResponseEntity<?> updateQuantity(@PathVariable("id") Long cartItemId,
                                            @RequestBody Map<String, Object> payload,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        Optional<User> userOpt = getCurrentUser();
        String cartToken = getOrCreateCartToken(request, response);

        Optional<CartItem> found = cartRepo.findById(cartItemId);
        if (found.isEmpty()) return ResponseEntity.noContent().build();

        CartItem ci = found.get();
        boolean allowed = (userOpt.isPresent() && ci.getUser() != null && Objects.equals(ci.getUser().getId(), userOpt.get().getId()))
                || (userOpt.isEmpty() && Objects.equals(cartToken, ci.getCartToken()));
        if (!allowed) {
            return ResponseEntity.status(403).body(Map.of("message", "Недостаточно прав для изменения элемента корзины"));
        }

        Integer qty = null;
        try {
            qty = payload.get("quantity") == null ? null : Integer.valueOf(String.valueOf(payload.get("quantity")));
        } catch (Exception ignored) {
        }
        if (qty == null) return ResponseEntity.badRequest().body(Map.of("message", "quantity обязателен"));

        if (qty <= 0) {
            cartRepo.delete(ci);
        } else {
            ci.setQuantity(qty);
            cartRepo.save(ci);
        }

        List<CartItem> updated = userOpt.map(u -> cartRepo.findByUser_Id(u.getId()))
                .orElseGet(() -> cartRepo.findByCartToken(cartToken));
        if (updated.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(toCartResponse(updated));
    }

    /**
     * Полностью очищает корзину: пользовательскую (по userId) или гостевую (по cart_token).
     */
    @DeleteMapping("/clear")
    @Operation(summary = "Очистить корзину", description = "Публично (гость/пользователь). Очищает корзину пользователя или гостевую по cart_token.")
    public ResponseEntity<?> clearCart(HttpServletRequest request, HttpServletResponse response) {
        Optional<User> userOpt = getCurrentUser();
        String cartToken = getOrCreateCartToken(request, response);
        if (userOpt.isPresent()) cartRepo.deleteByUser_Id(userOpt.get().getId());
        else cartRepo.deleteByCartToken(cartToken);
        return ResponseEntity.noContent().build();
    }

    private Optional<User> getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken)
                return Optional.empty();
            String username;
            Object principal = auth.getPrincipal();
            // Поддержка любых реализаций UserDetails (например, CustomUserDetails)
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails u) {
                username = u.getUsername();
            } else {
                username = String.valueOf(principal);
            }
            return userRepo.findByUsername(username);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String getOrCreateCartToken(HttpServletRequest request, HttpServletResponse response) {
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("cart_token".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    token = c.getValue();
                    break;
                }
            }
        }
        if (token == null) {
            token = UUID.randomUUID().toString();
        }
        // Refresh cookie to extend lifetime (sliding expiration)
        boolean secure = request.isSecure();
        ResponseCookie cookie = ResponseCookie.from("cart_token", token)
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .maxAge(60L * 60L * 24L * 365L) // 365 дней
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return token;
    }

    private Map<String, Object> toCartResponse(List<CartItem> items) {
        List<Map<String, Object>> list = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        Long currentBrandId = null;
        for (CartItem ci : items) {
            BigDecimal price = Optional.ofNullable(ci.getProduct())
                    .map(p -> p.getPromoPrice() != null && p.getPromoPrice().signum() > 0 ? p.getPromoPrice() : p.getPrice())
                    .orElse(BigDecimal.ZERO);
            int qty = Optional.ofNullable(ci.getQuantity()).orElse(0);
            total = total.add(price.multiply(BigDecimal.valueOf(qty)));
            Map<String, Object> row = new HashMap<>();
            row.put("id", ci.getId());
            row.put("productId", ci.getProduct() != null ? ci.getProduct().getId() : null);
            row.put("productName", ci.getProduct() != null ? ci.getProduct().getName() : null);
            row.put("quantity", qty);
            row.put("price", price);
            row.put("brandId", ci.getBrand() != null ? ci.getBrand().getId() : null);
            if (currentBrandId == null && ci.getBrand() != null) currentBrandId = ci.getBrand().getId();
            list.add(row);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("items", list);
        body.put("total", total);
        body.put("currentBrandId", currentBrandId);
        return body;
    }
}
