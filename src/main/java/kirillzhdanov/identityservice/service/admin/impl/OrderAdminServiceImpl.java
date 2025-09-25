package kirillzhdanov.identityservice.service.admin.impl;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import kirillzhdanov.identityservice.dto.order.OrderDto;
import kirillzhdanov.identityservice.dto.order.OrderItemDto;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.order.Order;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.order.OrderRepository;
import kirillzhdanov.identityservice.service.admin.OrderAdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class OrderAdminServiceImpl implements OrderAdminService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public OrderAdminServiceImpl(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static LocalDateTime parseDateStart(String s) {
        if (!StringUtils.hasText(s)) return null;
        try {
            // сначала ISO
            return LocalDateTime.parse(s);
        } catch (Exception ignored) {
        }
        try {
            LocalDate d = LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return d.atStartOfDay();
        } catch (Exception ignored) {
        }
        return null;
    }

    private static LocalDateTime parseDateEnd(String s) {
        if (!StringUtils.hasText(s)) return null;
        try {
            return LocalDateTime.parse(s);
        } catch (Exception ignored) {
        }
        try {
            LocalDate d = LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return d.atTime(23, 59, 59, 999_000_000);
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDto> findOrders(Pageable pageable, String search, Long brandId, String dateFrom, String dateTo) {
        Specification<Order> spec = Specification.where(null);

        // Фильтр: поиск
        if (StringUtils.hasText(search)) {
            spec = spec.and((root, query, cb) -> {
                var client = root.join("client", JoinType.LEFT);
                var brand = root.join("brand", JoinType.LEFT);
                String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
                Predicate byId = cb.disjunction();
                try {
                    long idVal = Long.parseLong(search.trim());
                    byId = cb.equal(root.get("id"), idVal);
                } catch (NumberFormatException ignored) {
                }
                Predicate byClient = cb.or(
                        cb.like(cb.lower(client.get("username")), like),
                        cb.like(cb.lower(client.get("firstName")), like),
                        cb.like(cb.lower(client.get("lastName")), like)
                );
                Predicate byBrand = cb.like(cb.lower(brand.get("name")), like);
                return cb.or(byId, byClient, byBrand);
            });
        }

        // Фильтр: brandId
        if (brandId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("brand").get("id"), brandId));
        }

        // Фильтр: даты
        LocalDateTime from = parseDateStart(dateFrom);
        LocalDateTime to = parseDateEnd(dateTo);
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        // Ограничение для ADMIN по masterId пользователя возможно после появления поля masterId у Brand
        // Long adminMasterId = resolveAdminMasterId();
        // if (adminMasterId != null) {
        //     spec = spec.and((root, q, cb2) -> cb2.equal(root.get("brand").get("masterId"), adminMasterId));
        // }

        Page<Order> page = orderRepository.findAll(spec, pageable);
        List<OrderDto> dtos = new ArrayList<>();
        for (Order o : page.getContent()) {
            dtos.add(mapOrder(o));
        }
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDto> findOrdersForBrands(Pageable pageable, String search, List<Long> brandIds, String dateFrom, String dateTo) {
        Specification<Order> spec = Specification.where(null);

        if (brandIds != null && !brandIds.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("brand").get("id").in(brandIds));
        } else {
            // если список пуст — ничего не отдаём
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // по аналогии добавим поиск и даты
        if (StringUtils.hasText(search)) {
            spec = spec.and((root, query, cb) -> {
                var client = root.join("client", JoinType.LEFT);
                var brand = root.join("brand", JoinType.LEFT);
                String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
                Predicate byId = cb.disjunction();
                try {
                    long idVal = Long.parseLong(search.trim());
                    byId = cb.equal(root.get("id"), idVal);
                } catch (NumberFormatException ignored) {
                }
                Predicate byClient = cb.or(
                        cb.like(cb.lower(client.get("username")), like),
                        cb.like(cb.lower(client.get("firstName")), like),
                        cb.like(cb.lower(client.get("lastName")), like)
                );
                Predicate byBrand = cb.like(cb.lower(brand.get("name")), like);
                return cb.or(byId, byClient, byBrand);
            });
        }

        LocalDateTime from = parseDateStart(dateFrom);
        LocalDateTime to = parseDateEnd(dateTo);
        if (from != null) spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        if (to != null) spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));

        Page<Order> page = orderRepository.findAll(spec, pageable);
        List<OrderDto> dtos = new ArrayList<>();
        for (Order o : page.getContent()) dtos.add(mapOrder(o));
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    private OrderDto mapOrder(Order o) {
        if (o == null) return null;
        Long clientId = Optional.ofNullable(o.getClient()).map(User::getId).orElse(null);
        String clientName = Optional.ofNullable(o.getClient()).map(u -> {
            String fn = safe(u.getFirstName());
            String ln = safe(u.getLastName());
            String un = safe(u.getUsername());
            String full = (ln + " " + fn).trim();
            return full.isBlank() ? un : full;
        }).orElse(null);
        String clientPhone = Optional.ofNullable(o.getClient()).map(User::getPhone).orElse(null);
        String clientEmail = Optional.ofNullable(o.getClient()).map(User::getEmail).orElse(null);
        Long brandId = Optional.ofNullable(o.getBrand()).map(Brand::getId).orElse(null);
        String brandName = Optional.ofNullable(o.getBrand()).map(Brand::getName).orElse(null);
        List<OrderItemDto> items = new ArrayList<>();
        if (o.getItems() != null) {
            o.getItems().forEach(oi -> items.add(OrderItemDto.builder()
                    .id(oi.getId())
                    .productId(oi.getProduct() != null ? oi.getProduct().getId() : null)
                    .productName(oi.getProduct() != null ? oi.getProduct().getName() : null)
                    .quantity(oi.getQuantity())
                    .price(Optional.ofNullable(oi.getPrice()).orElse(BigDecimal.ZERO))
                    .build()));
        }
        return OrderDto.builder()
                .id(o.getId())
                .clientId(clientId)
                .clientName(clientName)
                .clientPhone(clientPhone)
                .clientEmail(clientEmail)
                .brandId(brandId)
                .brandName(brandName)
                .status(o.getStatus() != null ? o.getStatus().name() : null)
                .total(Optional.ofNullable(o.getTotal()).orElse(BigDecimal.ZERO))
                .createdAt(o.getCreatedAt())
                .comment(o.getComment())
                .items(items)
                .build();
    }

    private Long resolveAdminMasterId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            boolean isOwner = hasRole(auth, "ROLE_OWNER") || hasRole(auth, "OWNER");
            boolean isAdmin = hasRole(auth, "ROLE_ADMIN") || hasRole(auth, "ADMIN");
            if (isOwner) return null; // без ограничений
            if (!isAdmin) return null; // клиент — ограничения не применяем здесь
            String username = auth.getName();
            Optional<User> u = userRepository.findByUsername(username);
            return u.map(User::getMasterId).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean hasRole(Authentication auth, String role) {
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (Objects.equals(ga.getAuthority(), role)) return true;
        }
        return false;
    }
}
