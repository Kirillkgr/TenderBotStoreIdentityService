package kirillzhdanov.identityservice.testutil;

import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.order.Order;
import kirillzhdanov.identityservice.model.order.OrderStatus;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.order.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile("dev")
public class TestOrderFactory {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private UserRepository userRepository;

    public Order createOrderForClient(String username, Long brandId) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Brand brand = resolveBrand(brandId);
        Order o = new Order();
        o.setClient(user);
        o.setBrand(brand);
        o.setCreatedAt(LocalDateTime.now());
        o.setStatus(OrderStatus.QUEUED);
        o.setTotal(java.math.BigDecimal.ZERO);
        return orderRepository.save(o);
    }

    public Long getAnyBrandId() {
        return resolveBrand(null).getId();
    }

    private Brand resolveBrand(Long brandId) {
        if (brandId != null) {
            return brandRepository.findById(brandId).orElseGet(this::firstBrandOrThrow);
        }
        return firstBrandOrThrow();
    }

    private Brand firstBrandOrThrow() {
        var page = brandRepository.findAll(PageRequest.of(0, 1, Sort.by("id").ascending()));
        if (!page.isEmpty()) return page.getContent().getFirst();
        throw new java.util.NoSuchElementException("No brands available for test");
    }

    public Order setStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(status);
        return orderRepository.save(order);
    }
}
