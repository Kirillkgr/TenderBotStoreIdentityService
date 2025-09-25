package kirillzhdanov.identityservice.service.impl;

import jakarta.transaction.Transactional;
import kirillzhdanov.identityservice.model.cart.CartItem;
import kirillzhdanov.identityservice.repository.ProductRepository;
import kirillzhdanov.identityservice.repository.cart.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartInterestScheduler {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    // Каждые 10 минут обрабатываем гостевые корзины старше 2 часов
    @Scheduled(fixedDelay = 10 * 60 * 1000L, initialDelay = 60 * 1000L)
    @Transactional
    public void processExpiredGuestCarts() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(2);
        List<CartItem> expired = cartItemRepository.findByUser_IsNullAndUpdatedAtBefore(threshold);
        if (expired.isEmpty()) return;
        log.info("Cart interest: processing {} expired guest items (threshold={})", expired.size(), threshold);
        for (CartItem ci : expired) {
            try {
                if (ci.getProduct() != null) {
                    long inc = ci.getQuantity() == null ? 1L : Math.max(1L, ci.getQuantity());
                    var p = ci.getProduct();
                    p.setAnonymousCartInterest((p.getAnonymousCartInterest() == null ? 0L : p.getAnonymousCartInterest()) + inc);
                    productRepository.save(p);
                }
                cartItemRepository.delete(ci); // очищаем анонимную корзину после фиксации
            } catch (Exception e) {
                log.warn("Cart interest: failed to process cartItem id={}", ci.getId(), e);
            }
        }
    }
}
