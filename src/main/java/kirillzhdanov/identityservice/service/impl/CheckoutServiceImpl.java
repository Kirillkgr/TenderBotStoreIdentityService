package kirillzhdanov.identityservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.cart.CartItem;
import kirillzhdanov.identityservice.model.order.DeliveryMode;
import kirillzhdanov.identityservice.model.order.Order;
import kirillzhdanov.identityservice.model.order.OrderItem;
import kirillzhdanov.identityservice.model.pickup.PickupPoint;
import kirillzhdanov.identityservice.model.userbrand.DeliveryAddress;
import kirillzhdanov.identityservice.repository.cart.CartItemRepository;
import kirillzhdanov.identityservice.repository.order.OrderItemRepository;
import kirillzhdanov.identityservice.repository.order.OrderRepository;
import kirillzhdanov.identityservice.repository.pickup.PickupPointRepository;
import kirillzhdanov.identityservice.repository.userbrand.DeliveryAddressRepository;
import kirillzhdanov.identityservice.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final CartItemRepository cartRepo;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final PickupPointRepository pickupPointRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Order createOrderFromCart(User user, DeliveryMode mode, Long addressId, Long pickupPointId, String comment) {
        if (user == null) throw new IllegalArgumentException("User is required");
        List<CartItem> items = cartRepo.findByUser_Id(user.getId());
        if (items.isEmpty()) throw new IllegalStateException("Cart is empty");

        // Бренд из первой позиции и проверим однородность
        Brand brand = items.get(0).getBrand();
        Long brandId = brand != null ? brand.getId() : null;
        for (CartItem ci : items) {
            if (ci.getBrand() != null && brandId != null && !Objects.equals(ci.getBrand().getId(), brandId)) {
                throw new IllegalStateException("Mixed brand cart is not allowed");
            }
        }

        Order order = new Order();
        order.setClient(user);
        order.setBrand(brand);
        order.setDeliveryMode(mode);
        order.setComment(comment);

        // Snapshots
        try {
            if (mode == DeliveryMode.DELIVERY) {
                if (addressId == null) throw new IllegalArgumentException("addressId is required for DELIVERY");
                Optional<DeliveryAddress> addrOpt = deliveryAddressRepository.findById(addressId);
                if (addrOpt.isEmpty()) throw new IllegalArgumentException("Address not found");
                DeliveryAddress addr = addrOpt.get();
                Map<String, Object> snap = new HashMap<>();
                snap.put("id", addr.getId());
                snap.put("line1", addr.getLine1());
                snap.put("line2", addr.getLine2());
                snap.put("city", addr.getCity());
                snap.put("region", addr.getRegion());
                snap.put("postcode", addr.getPostcode());
                snap.put("comment", addr.getComment());
                order.setAddressSnapshot(objectMapper.writeValueAsString(snap));
            } else if (mode == DeliveryMode.PICKUP) {
                if (pickupPointId == null) throw new IllegalArgumentException("pickupPointId is required for PICKUP");
                Optional<PickupPoint> ppOpt = pickupPointRepository.findById(pickupPointId);
                if (ppOpt.isEmpty()) throw new IllegalArgumentException("Pickup point not found");
                PickupPoint pp = ppOpt.get();
                Map<String, Object> snap = new HashMap<>();
                snap.put("id", pp.getId());
                snap.put("name", pp.getName());
                snap.put("address", pp.getAddress());
                order.setPickupSnapshot(objectMapper.writeValueAsString(snap));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize snapshot", e);
        }

        // Compute totals and items
        BigDecimal total = BigDecimal.ZERO;
        order = orderRepository.save(order);
        for (CartItem ci : items) {
            if (ci.getProduct() == null) continue;
            BigDecimal price = ci.getProduct().getPromoPrice() != null && ci.getProduct().getPromoPrice().signum() > 0
                    ? ci.getProduct().getPromoPrice() : ci.getProduct().getPrice();
            int qty = Optional.ofNullable(ci.getQuantity()).orElse(0);
            total = total.add(price.multiply(BigDecimal.valueOf(qty)));

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(ci.getProduct());
            oi.setQuantity(qty);
            oi.setPrice(price);
            orderItemRepository.save(oi);
        }
        order.setTotal(total);
        order = orderRepository.save(order);

        // Очистим корзину
        cartRepo.deleteByUser_Id(user.getId());

        return order;
    }
}
