package kirillzhdanov.identityservice.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.cart.CartItem;
import kirillzhdanov.identityservice.model.product.Product;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.cart.CartItemRepository;
import kirillzhdanov.identityservice.repository.ProductRepository;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.model.Brand;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;

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

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCart(HttpServletRequest request, HttpServletResponse response) {
        try {
            Optional<User> userOpt = getCurrentUser();
            String cartToken = getOrCreateCartToken(request, response);

            // Do not validate identifiers on GET; only return canonical identifiers back to the client

            List<CartItem> items = userOpt.map(u -> cartRepo.findByUser_Id(u.getId()))
                    .orElseGet(() -> cartRepo.findByCartToken(cartToken));

            Map<String, Object> body = toCartResponse(items);
            // Добавим идентификаторы корзины для синхронизации клиента
            String scopeId = userOpt.map(u -> "user:" + u.getId()).orElse("guest:" + cartToken);
            body.put("cartToken", userOpt.isPresent() ? null : cartToken);
            body.put("cartScopeId", scopeId);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("/cart GET failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "code", "CART_FETCH_ERROR",
                    "message", "Не удалось получить корзину"
            ));
        }
    }

    @PostMapping("/add")
    @Transactional
    public ResponseEntity<?> addToCart(@RequestBody Map<String, Object> payload,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        try {
            Long productId = payload.get("productId") == null ? null : Long.valueOf(String.valueOf(payload.get("productId")));
            Integer quantity = payload.get("quantity") == null ? 1 : Integer.valueOf(String.valueOf(payload.get("quantity")));
            if (productId == null || quantity == null || quantity <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "productId и quantity обязательны"));
            }

            Optional<Product> productOpt = productRepo.findById(productId);
            if (productOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Товар не найден"));
            }
            Product product = productOpt.get();

            // Определим бренд товара (если в сущности нет - попробуем из контекста/заголовка)
            Brand productBrand = product.getBrand();
            if (productBrand == null) {
                // Попробуем взять brandId из заголовка (X-Brand-Id) или query и найти бренд в БД
                String header = request.getHeader("X-Brand-Id");
                Long bId = null;
                try {
                    if (header != null && !header.isBlank()) bId = Long.valueOf(header);
                } catch (Exception ignored) {
                }
                if (bId == null) {
                    try {
                        String q = request.getParameter("brand");
                        if (q != null && !q.isBlank()) bId = Long.valueOf(q);
                    } catch (Exception ignored) {
                    }
                }
                if (bId != null) {
                    productBrand = brandRepo.findById(bId).orElse(null);
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
            String scopeId = userOpt.map(u -> "user:" + u.getId()).orElse("guest:" + cartToken);
            resp.put("cartToken", userOpt.isPresent() ? null : cartToken);
            resp.put("cartScopeId", scopeId);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("/cart/add failed", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Не удалось добавить товар", "error", e.getClass().getSimpleName()));
        }
    }

    @DeleteMapping("/remove/{id}")
    @Transactional
    public ResponseEntity<?> removeFromCart(@PathVariable("id") Long cartItemId,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        Optional<User> userOpt = getCurrentUser();
        String cartToken = getOrCreateCartToken(request, response);

        Optional<CartItem> found = cartRepo.findById(cartItemId);
        if (found.isEmpty()) return ResponseEntity.noContent().build();

        CartItem ci = found.get();
        boolean allowed = (userOpt.isPresent() && ci.getUser() != null && Objects.equals(ci.getUser().getId(), userOpt.get().getId()))
                || (!userOpt.isPresent() && Objects.equals(cartToken, ci.getCartToken()));
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

    @PatchMapping("/item/{id}")
    @Transactional
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
                || (!userOpt.isPresent() && Objects.equals(cartToken, ci.getCartToken()));
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

    @DeleteMapping("/clear")
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
            String username = String.valueOf(auth.getPrincipal());
            // В зависимости от реализации principal может быть UserDetails — упростим:
            if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User u) {
                username = u.getUsername();
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
            ResponseCookie cookie = ResponseCookie.from("cart_token", token)
                    .path("/")
                    .httpOnly(false)
                    .secure(false)
                    .maxAge(60L * 60L * 24L * 30L) // 30 дней
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
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
