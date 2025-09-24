package kirillzhdanov.identityservice.service.impl;

import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.cart.CartItem;
import kirillzhdanov.identityservice.repository.cart.CartItemRepository;
import kirillzhdanov.identityservice.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;

    @Override
    public void mergeGuestCartToUser(User user, String cartToken) {
        if (user == null || cartToken == null || cartToken.isBlank()) return;

        List<CartItem> guest = cartItemRepository.findByCartToken(cartToken);
        if (guest.isEmpty()) {
            log.debug("Cart merge: no guest items for token={}", cartToken);
            return;
        }

        List<CartItem> userItems = cartItemRepository.findByUser_Id(user.getId());
        log.info("Cart merge start: user={}, guestItems={}, userItems={}", user.getUsername(), guest.size(), userItems.size());

        // Проверим бренды: если конфликт — предпочитаем корзину пользователя, гость — очищаем
        Long guestBrand = guest.stream().findFirst().map(ci -> ci.getBrand() != null ? ci.getBrand().getId() : null).orElse(null);
        Long userBrand = userItems.stream().findFirst().map(ci -> ci.getBrand() != null ? ci.getBrand().getId() : null).orElse(null);
        if (guestBrand != null && userBrand != null && !Objects.equals(guestBrand, userBrand)) {
            // Конфликт бренд-скоупа: оставим корзину пользователя как источник истины
            log.warn("Cart merge brand conflict: guestBrand={}, userBrand={}, keeping user cart", guestBrand, userBrand);
            cartItemRepository.deleteByCartToken(cartToken);
            return;
        }

        // Сольём позиции: по product_id увеличиваем количество
        Map<Long, CartItem> byProduct = new HashMap<>();
        for (CartItem ci : userItems) {
            if (ci.getProduct() != null) byProduct.put(ci.getProduct().getId(), ci);
        }
        for (CartItem g : guest) {
            if (g.getProduct() == null) continue;
            CartItem existing = byProduct.get(g.getProduct().getId());
            if (existing != null) {
                int q = (existing.getQuantity() == null ? 0 : existing.getQuantity()) + (g.getQuantity() == null ? 0 : g.getQuantity());
                existing.setQuantity(q);
                cartItemRepository.save(existing);
                // удалить гостевую запись после переноса
                cartItemRepository.delete(g);
                log.debug("Cart merge: summed productId={} to qty={}", g.getProduct().getId(), q);
            } else {
                // Переносим как пользовательскую позицию
                g.setUser(user);
                g.setCartToken(null);
                cartItemRepository.save(g);
                byProduct.put(g.getProduct().getId(), g);
                log.debug("Cart merge: moved productId={} qty={}", g.getProduct().getId(), g.getQuantity());
            }
        }
        log.info("Cart merge complete: user={}, finalItems={} ", user.getUsername(), cartItemRepository.findByUser_Id(user.getId()).size());
    }
}
