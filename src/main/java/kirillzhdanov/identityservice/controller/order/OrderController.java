package kirillzhdanov.identityservice.controller.order;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.order.CourierMessageRequest;
import kirillzhdanov.identityservice.dto.order.OrderDto;
import kirillzhdanov.identityservice.dto.order.OrderItemDto;
import kirillzhdanov.identityservice.dto.order.UpdateOrderStatusRequest;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.order.Order;
import kirillzhdanov.identityservice.model.order.OrderMessage;
import kirillzhdanov.identityservice.model.order.OrderStatus;
import kirillzhdanov.identityservice.notification.longpoll.LongPollService;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.order.OrderMessageRepository;
import kirillzhdanov.identityservice.repository.order.OrderRepository;
import kirillzhdanov.identityservice.repository.order.OrderReviewRepository;
import kirillzhdanov.identityservice.repository.userbrand.UserBrandMembershipRepository;
import kirillzhdanov.identityservice.service.admin.OrderAdminService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Контроллер управления заказами для клиентов и персонала бренда.
 * <p>
 * Здесь можно посмотреть свои заказы, увидеть заказы бренда (при наличии прав),
 * менять статусы (для персонала) и обмениваться сообщениями по заказу.
 */
@RestController
@RequestMapping("/order/v1")
@RequiredArgsConstructor
public class OrderController {

    private final OrderAdminService orderAdminService;
    private final OrderRepository orderRepository;
    private final OrderMessageRepository orderMessageRepository;
    private final OrderReviewRepository orderReviewRepository;
    private final UserRepository userRepository;
    private final UserBrandMembershipRepository membershipRepository;
    private final LongPollService longPollService;

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Возвращает заказы текущего пользователя (клиента), отсортированные по убыванию.
     */
    @GetMapping("/my")
    @Transactional(readOnly = true)
    @Operation(summary = "Мои заказы", description = "Требуется аутентификация. Возвращает заказы текущего пользователя.")
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
            Integer rating = null;
            String reviewComment = null;
            try {
                var rev = orderReviewRepository.findByOrder_Id(o.getId()).orElse(null);
                if (rev != null) {
                    rating = rev.getRating();
                    reviewComment = rev.getComment();
                }
            } catch (Exception ignored) {
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
                    .rating(rating)
                    .reviewComment(reviewComment)
                    .build();
        }).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Возвращает страницу заказов по тем брендам, где у пользователя есть членство (персонал/админ).
     * Используется для рабочих экранов персонала бренда.
     */
    @GetMapping("/orders")
    @Operation(summary = "Доступные заказы (по membership)", description = "Требуется аутентификация. Возвращает заказы по брендам, где у пользователя есть membership.")
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

    /**
     * Возвращает страницу заказов конкретного бренда, если у пользователя есть права (membership) у этого бренда.
     */
    @GetMapping("/brand/{brandId}/orders")
    @Operation(summary = "Заказы бренда", description = "Требуется аутентификация. Доступ только при наличии membership у бренда; иначе 403.")
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

    /**
     * Смена статуса заказа персоналом бренда (при наличии прав).
     * Список допустимых переходов валидируется сервером.
     */
    @PatchMapping("/orders/{id}/status")
    @Operation(summary = "Смена статуса заказа", description = "Требуется аутентификация и membership у бренда заказа. Рекомендовано ограничить ролями (CASHIER/COOK/ADMIN/OWNER).")
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
        Long brandId = Optional.ofNullable(order.getBrand()).map(Brand::getId).orElse(null);
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
        Optional.ofNullable(order.getClient()).map(User::getId).ifPresent(clientId -> longPollService.publishStatusChanged(clientId, order.getId(), oldStatus.name(), newStatus.name()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Персонал отправляет сообщение клиенту по заказу.
     */
    @PostMapping("/orders/{id}/message")
    @Operation(summary = "Сообщение курьера/персонала клиенту", description = "Требуется аутентификация и membership у бренда заказа.")
    public ResponseEntity<Void> sendCourierMessage(@PathVariable Long id,
                                                   @Valid @RequestBody CourierMessageRequest req,
                                                   Authentication authentication) {
        if (!isAuthenticated(authentication)) return ResponseEntity.status(401).build();
        Optional<Order> opt = orderRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Order order = opt.get();
        User user = resolveUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();
        Long brandId = Optional.ofNullable(order.getBrand()).map(Brand::getId).orElse(null);
        if (brandId == null) return ResponseEntity.status(409).build();
        boolean allowed = membershipRepository.findByUser_IdAndBrand_Id(user.getId(), brandId).isPresent();
        if (!allowed) return ResponseEntity.status(403).build();
        Long clientId = Optional.ofNullable(order.getClient()).map(User::getId).orElse(null);
        if (clientId != null) {
            // persist message (admin/staff -> client)
            OrderMessage saved = orderMessageRepository.save(OrderMessage.builder()
                    .order(order)
                    .fromClient(false)
                    .text(req.getText())
                    .senderUserId(user.getId())
                    .build());
            Long msgId = Optional.of(saved).map(OrderMessage::getId).orElse(null);
            longPollService.publishCourierMessage(clientId, order.getId(), req.getText(), msgId);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Клиент отправляет сообщение персоналу бренда по своему заказу.
     */
    @PostMapping("/orders/{id}/client-message")
    @Operation(summary = "Сообщение клиента персоналу", description = "Требуется аутентификация. Разрешено только клиенту данного заказа.")
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
        Long brandId = Optional.ofNullable(order.getBrand()).map(Brand::getId).orElse(null);
        // Сохраняем сообщение (client -> admin)
        OrderMessage saved = orderMessageRepository.save(OrderMessage.builder()
                .order(order)
                .fromClient(true)
                .text(req.getText())
                .senderUserId(user.getId())
                .build());
        Long msgId = Optional.of(saved).map(OrderMessage::getId).orElse(null);
        // Оповестим персонал бренда: всем участникам membership по brandId
        if (brandId != null) {
            var userIds = membershipRepository.findUserIdsByBrandId(brandId);
            if (userIds != null) {
                java.util.Set<Long> unique = new java.util.HashSet<>();
                for (Long uid : userIds) {
                    if (uid == null) continue;
                    if (uid.equals(clientId)) continue; // не уведомляем отправителя (клиента)
                    if (unique.add(uid)) { // только первый раз
                        longPollService.publishClientMessage(uid, order.getId(), req.getText(), msgId);
                    }
                }
            }
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Клиент оставляет отзыв по заказу после завершения. Повторная отправка запрещена.
     */
    @PostMapping("/orders/{id}/review")
    @Operation(summary = "Отправить отзыв по заказу", description = "Требуется аутентификация. Разрешено клиенту заказа после статуса COMPLETED. Повторная отправка запрещена.")
    public ResponseEntity<Void> submitReview(@PathVariable Long id,
                                             @Valid @RequestBody ReviewRequest req,
                                             Authentication authentication) {
        if (!isAuthenticated(authentication)) return ResponseEntity.status(401).build();
        if (req == null || req.getRating() == null || req.getRating() < 1 || req.getRating() > 5) {
            return ResponseEntity.badRequest().build();
        }
        Optional<Order> opt = orderRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Order order = opt.get();
        User user = resolveUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();
        // Только клиент этого заказа может оставлять отзыв
        Long clientId = Optional.ofNullable(order.getClient()).map(User::getId).orElse(null);
        if (clientId == null || !clientId.equals(user.getId())) return ResponseEntity.status(403).build();
        // Отзыв разрешён только после завершения заказа
        if (order.getStatus() == null || order.getStatus() != OrderStatus.COMPLETED) {
            return ResponseEntity.status(409).build();
        }
        // Запрещаем повторную отправку отзыва
        var existing = orderReviewRepository.findByOrder_Id(order.getId()).orElse(null);
        if (existing != null) return ResponseEntity.status(409).build();

        orderReviewRepository.save(kirillzhdanov.identityservice.model.order.OrderReview.builder()
                .order(order)
                .client(user)
                .rating(req.getRating())
                .comment(req.getComment())
                .createdAt(java.time.LocalDateTime.now())
                .build());
        return ResponseEntity.noContent().build();
    }

    /**
     * Сообщения по заказу: видны клиенту заказа и персоналу бренда, которому принадлежит заказ.
     */
    @GetMapping("/orders/{id}/messages")
    @Operation(summary = "Сообщения заказа", description = "Требуется аутентификация. Доступ клиенту заказа и сотрудникам бренда (membership).")
    public ResponseEntity<?> getOrderMessages(@PathVariable Long id,
                                              Authentication authentication) {
        if (!isAuthenticated(authentication)) return ResponseEntity.status(401).build();
        Optional<Order> opt = orderRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Order order = opt.get();
        User user = resolveUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();
        // Разрешаем: клиент своего заказа или сотрудник бренда
        Long clientId = Optional.ofNullable(order.getClient()).map(User::getId).orElse(null);
        Long brandId = Optional.ofNullable(order.getBrand()).map(Brand::getId).orElse(null);
        boolean allowed = (clientId != null && clientId.equals(user.getId()))
                || (brandId != null && membershipRepository.findByUser_IdAndBrand_Id(user.getId(), brandId).isPresent());
        if (!allowed) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(orderMessageRepository.findByOrder_IdOrderByIdAsc(id));
    }

    private boolean isValidTransition(OrderStatus from, OrderStatus to) {
        if (from == to) return true;
        return switch (from) {
            case QUEUED -> to == OrderStatus.PREPARING || to == OrderStatus.CANCELED;
            case PREPARING ->
                    to == OrderStatus.READY_FOR_PICKUP || to == OrderStatus.OUT_FOR_DELIVERY || to == OrderStatus.CANCELED;
            case READY_FOR_PICKUP ->
                    to == OrderStatus.DELIVERED || to == OrderStatus.COMPLETED || to == OrderStatus.CANCELED;
            case OUT_FOR_DELIVERY -> to == OrderStatus.DELIVERED || to == OrderStatus.CANCELED;
            case DELIVERED -> to == OrderStatus.COMPLETED;
            case COMPLETED, CANCELED -> false;
        };
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.isAuthenticated();
    }

    private User resolveUser(Authentication authentication) {
        try {
            String username = authentication.getName();
            return userRepository.findByUsername(username).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Data
    public static class ReviewRequest {
        private Integer rating; // 1..5
        private String comment;
    }
}
