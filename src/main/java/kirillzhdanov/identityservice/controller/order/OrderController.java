package kirillzhdanov.identityservice.controller.order;

import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.order.CourierMessageRequest;
import kirillzhdanov.identityservice.dto.order.OrderItemDto;
import kirillzhdanov.identityservice.dto.order.OrderDto;
import kirillzhdanov.identityservice.dto.order.UpdateOrderStatusRequest;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.order.Order;
import kirillzhdanov.identityservice.model.order.OrderMessage;
import kirillzhdanov.identityservice.model.order.OrderStatus;
import kirillzhdanov.identityservice.notification.longpoll.LongPollService;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.order.OrderRepository;
import kirillzhdanov.identityservice.repository.order.OrderMessageRepository;
import kirillzhdanov.identityservice.repository.userbrand.UserBrandMembershipRepository;
import kirillzhdanov.identityservice.service.admin.OrderAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RestController
@RequestMapping("/order/v1")
@RequiredArgsConstructor
public class OrderController {

    private final OrderAdminService orderAdminService;
    private final OrderRepository orderRepository;
    private final OrderMessageRepository orderMessageRepository;
    private final UserRepository userRepository;
    private final UserBrandMembershipRepository membershipRepository;
    private final LongPollService longPollService;

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    @GetMapping("/my")
    @Transactional(readOnly = true)
    public ResponseEntity<?> myOrders(Authentication authentication) {
        if (!isAuthenticated(authentication)) return ResponseEntity.status(401).build();
        User user = resolveUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();
        var list = orderRepository.findByClient_IdOrderByIdDesc(user.getId());
        var dtos = list.stream().map(o -> {
            java.util.List<OrderItemDto> items = new java.util.ArrayList<>();
            if (o.getItems() != null) {
                o.getItems().forEach(oi -> items.add(OrderItemDto.builder()
                        .id(oi.getId())
                        .productId(oi.getProduct() != null ? oi.getProduct().getId() : null)
                        .productName(oi.getProduct() != null ? oi.getProduct().getName() : null)
                        .quantity(oi.getQuantity())
                        .price(oi.getPrice())
                        .build()));
            }
            return OrderDto.builder()
                    .id(o.getId())
                    .clientId(user.getId())
                    .clientName(((o.getClient() != null ? (safe(o.getClient().getLastName()) + " " + safe(o.getClient().getFirstName())) : "")).trim())
                    .clientPhone(o.getClient() != null ? o.getClient().getPhone() : null)
                    .clientEmail(o.getClient() != null ? o.getClient().getEmail() : null)
                    .brandId(o.getBrand() != null ? o.getBrand().getId() : null)
                    .brandName(o.getBrand() != null ? o.getBrand().getName() : null)
                    .status(o.getStatus() != null ? o.getStatus().name() : null)
                    .deliveryMode(o.getDeliveryMode() != null ? o.getDeliveryMode().name() : null)
                    .total(o.getTotal())
                    .createdAt(o.getCreatedAt())
                    .comment(o.getComment())
                    .items(items)
                    .build();
        }).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/orders")
    public ResponseEntity<Page<OrderDto>> getAccessibleOrders(Pageable pageable,
                                                              Authentication authentication,
                                                              @RequestParam(required = false) String search,
                                                              @RequestParam(required = false) String dateFrom,
                                                              @RequestParam(required = false) String dateTo) {
        if (!isAuthenticated(authentication)) return ResponseEntity.status(401).build();
        User user = resolveUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();
        var brandIds = membershipRepository.findBrandIdsByUserId(user.getId());
        if (brandIds == null || brandIds.isEmpty()) {
            return ResponseEntity.ok(Page.empty(pageable));
        }
        Page<OrderDto> page = orderAdminService.findOrdersForBrands(pageable, search, brandIds, dateFrom, dateTo);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/brand/{brandId}/orders")
    public ResponseEntity<Page<OrderDto>> getBrandOrders(@PathVariable Long brandId,
                                                         Pageable pageable,
                                                         Authentication authentication) {
        if (!isAuthenticated(authentication)) return ResponseEntity.status(401).build();
        User user = resolveUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();
        // check membership
        boolean allowed = membershipRepository.findByUser_IdAndBrand_Id(user.getId(), brandId).isPresent();
        if (!allowed) return ResponseEntity.status(403).build();
        Page<OrderDto> page = orderAdminService.findOrders(pageable, null, brandId, null, null);
        return ResponseEntity.ok(page);
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id,
                                             @Valid @RequestBody UpdateOrderStatusRequest req,
                                             Authentication authentication) {
        if (!isAuthenticated(authentication)) return ResponseEntity.status(401).build();
        Optional<Order> opt = orderRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Order order = opt.get();
        User user = resolveUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();
        // check membership for order's brand
        Long brandId = Optional.ofNullable(order.getBrand()).map(b -> b.getId()).orElse(null);
        if (brandId == null) return ResponseEntity.status(409).build();
        boolean allowed = membershipRepository.findByUser_IdAndBrand_Id(user.getId(), brandId).isPresent();
        if (!allowed) return ResponseEntity.status(403).build();
        // validate transition
        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus = req.getNewStatus();
        if (!isValidTransition(oldStatus, newStatus)) {
            return ResponseEntity.status(409).build();
        }
        order.setStatus(newStatus);
        orderRepository.save(order);
        // publish user notification
        Long clientId = Optional.ofNullable(order.getClient()).map(User::getId).orElse(null);
        if (clientId != null) {
            longPollService.publishStatusChanged(clientId, order.getId(), oldStatus.name(), newStatus.name());
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders/{id}/message")
    public ResponseEntity<Void> sendCourierMessage(@PathVariable Long id,
                                                   @Valid @RequestBody CourierMessageRequest req,
                                                   Authentication authentication) {
        if (!isAuthenticated(authentication)) return ResponseEntity.status(401).build();
        Optional<Order> opt = orderRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Order order = opt.get();
        User user = resolveUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();
        Long brandId = Optional.ofNullable(order.getBrand()).map(b -> b.getId()).orElse(null);
        if (brandId == null) return ResponseEntity.status(409).build();
        boolean allowed = membershipRepository.findByUser_IdAndBrand_Id(user.getId(), brandId).isPresent();
        if (!allowed) return ResponseEntity.status(403).build();
        Long clientId = Optional.ofNullable(order.getClient()).map(User::getId).orElse(null);
        if (clientId != null) {
            // persist message (admin/staff -> client)
            orderMessageRepository.save(OrderMessage.builder()
                    .order(order)
                    .fromClient(false)
                    .text(req.getText())
                    .senderUserId(user.getId())
                    .build());
            longPollService.publishCourierMessage(clientId, order.getId(), req.getText());
        }
        return ResponseEntity.noContent().build();
    }

    // Клиент пишет администратору/персоналу бренда по заказу
    @PostMapping("/orders/{id}/client-message")
    public ResponseEntity<Void> sendClientMessage(@PathVariable Long id,
                                                  @Valid @RequestBody CourierMessageRequest req, // reuse dto with 'text'
                                                  Authentication authentication) {
        if (!isAuthenticated(authentication)) return ResponseEntity.status(401).build();
        Optional<Order> opt = orderRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Order order = opt.get();
        User user = resolveUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();
        // проверяем, что это клиент заказа
        Long clientId = Optional.ofNullable(order.getClient()).map(User::getId).orElse(null);
        if (clientId == null || !clientId.equals(user.getId())) return ResponseEntity.status(403).build();
        // заказ не должен быть завершён/отменён
        if (order.getStatus() != null) {
            switch (order.getStatus()) {
                case COMPLETED:
                case CANCELED:
                    return ResponseEntity.status(409).build();
            }
        }
        Long brandId = Optional.ofNullable(order.getBrand()).map(b -> b.getId()).orElse(null);
        // Сохраняем сообщение (client -> admin)
        orderMessageRepository.save(OrderMessage.builder()
                .order(order)
                .fromClient(true)
                .text(req.getText())
                .senderUserId(user.getId())
                .build());
        // Оповестим персонал бренда: всем участникам membership по brandId
        if (brandId != null) {
            var userIds = membershipRepository.findUserIdsByBrandId(brandId);
            if (userIds != null) {
                for (Long uid : userIds) {
                    if (uid != null) longPollService.publishClientMessage(uid, order.getId(), req.getText());
                }
            }
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/orders/{id}/messages")
    public ResponseEntity<?> getOrderMessages(@PathVariable Long id,
                                              Authentication authentication) {
        if (!isAuthenticated(authentication)) return ResponseEntity.status(401).build();
        Optional<Order> opt = orderRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Order order = opt.get();
        User user = resolveUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();
        Long clientId = Optional.ofNullable(order.getClient()).map(User::getId).orElse(null);
        Long brandId = Optional.ofNullable(order.getBrand()).map(b -> b.getId()).orElse(null);
        boolean isClient = clientId != null && clientId.equals(user.getId());
        boolean isStaff = brandId != null && membershipRepository.findByUser_IdAndBrand_Id(user.getId(), brandId).isPresent();
        if (!isClient && !isStaff) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(orderMessageRepository.findByOrder_IdOrderByIdAsc(id));
    }

    private boolean isAuthenticated(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    private User resolveUser(Authentication authentication) {
        try {
            String username = authentication.getName();
            return userRepository.findByUsername(username).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isValidTransition(OrderStatus from, OrderStatus to) {
        if (from == to) return true;
        switch (from) {
            case QUEUED:
                return to == OrderStatus.PREPARING || to == OrderStatus.CANCELED;
            case PREPARING:
                return to == OrderStatus.READY_FOR_PICKUP || to == OrderStatus.OUT_FOR_DELIVERY || to == OrderStatus.CANCELED;
            case READY_FOR_PICKUP:
                return to == OrderStatus.DELIVERED || to == OrderStatus.COMPLETED || to == OrderStatus.CANCELED;
            case OUT_FOR_DELIVERY:
                return to == OrderStatus.DELIVERED || to == OrderStatus.CANCELED;
            case DELIVERED:
                return to == OrderStatus.COMPLETED;
            case COMPLETED:
            case CANCELED:
                return false;
            default:
                return false;
        }
    }
}
