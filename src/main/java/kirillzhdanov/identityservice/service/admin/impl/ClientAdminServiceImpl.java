package kirillzhdanov.identityservice.service.admin.impl;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import kirillzhdanov.identityservice.dto.client.ClientDto;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.order.Order;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.order.OrderRepository;
import kirillzhdanov.identityservice.service.admin.ClientAdminService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ClientAdminServiceImpl implements ClientAdminService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public ClientAdminServiceImpl(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientDto> findClients(Pageable pageable, String search, Long masterIdParam) {
        Specification<User> spec = Specification.allOf();

        // Поиск по ФИО/логину/email/телефону
        if (StringUtils.hasText(search)) {
            spec = spec.and((root, query, cb) -> {
                String like = "%" + search.toLowerCase() + "%";
                Predicate byLogin = cb.like(cb.lower(root.get("username")), like);
                Predicate byFio = cb.or(
                        cb.like(cb.lower(root.get("lastName")), like),
                        cb.like(cb.lower(root.get("firstName")), like),
                        cb.like(cb.lower(root.get("patronymic")), like)
                );
                Predicate byEmail = cb.like(cb.lower(root.get("email")), like);
                Predicate byPhone = cb.like(cb.lower(root.get("phone")), like);
                return cb.or(byLogin, byFio, byEmail, byPhone);
            });
        }

        // Ограничение области видимости: по заказам мастера
        // ADMIN: автоматически ограничен своим masterId.
        // OWNER: может явно передать masterIdParam.
        Long adminMasterId = resolveAdminMasterId();
        Long effectiveMasterId = adminMasterId != null ? adminMasterId : masterIdParam;

        if (effectiveMasterId != null) {
            spec = spec.and((root, query, cb) -> {
                // exists(select 1 from Order o where o.client.id = root.id and o.master.id = :effectiveMasterId)
                Subquery<Long> sq = query.subquery(Long.class);
                Root<Order> o = sq.from(Order.class);
                sq.select(cb.literal(1L));
                Predicate byClientId = cb.equal(o.get("client").get("id"), root.get("id"));
                Predicate byMaster = cb.equal(o.get("master").get("id"), effectiveMasterId);
                Predicate masterNotNull = cb.isNotNull(o.get("master").get("id"));
                sq.where(cb.and(byClientId, byMaster, masterNotNull));
                return cb.exists(sq);
            });
        } else {
            // Без указания masterId (и не ADMIN) — ничего не показываем, чтобы не выходить за область видимости
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // Единый запрос с проекцией: клиенты мастера с датой и брендом последнего заказа
        Page<kirillzhdanov.identityservice.dto.client.ClientProjection> projPage =
                orderRepository.findClientsByMasterWithLastOrder(effectiveMasterId, StringUtils.hasText(search) ? search : null, pageable);

        List<ClientDto> list = new ArrayList<>(projPage.getNumberOfElements());
        for (var p : projPage.getContent()) {
            list.add(ClientDto.builder()
                    .id(p.getId())
                    .firstName(p.getFirstName())
                    .lastName(p.getLastName())
                    .patronymic(p.getPatronymic())
                    .email(p.getEmail())
                    .phone(p.getPhone())
                    .dateOfBirth(p.getDateOfBirth())
                    .lastOrderAt(p.getLastOrderAt())
                    .lastOrderBrandId(p.getLastOrderBrandId())
                    .lastOrderBrand(p.getLastOrderBrand())
                    .build());
        }
        return new PageImpl<>(list, pageable, projPage.getTotalElements());
    }

    private Long resolveAdminMasterId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            boolean isOwner = hasRole(auth, "ROLE_OWNER") || hasRole(auth, "OWNER");
            boolean isAdmin = hasRole(auth, "ROLE_ADMIN") || hasRole(auth, "ADMIN");
            String username = auth.getName();
            Optional<User> uOpt = userRepository.findByUsername(username);
            // И ADMIN, и OWNER — ограничиваем их собственным masterId (если он задан)
            if (isAdmin || isOwner) {
                if (uOpt.isEmpty()) return null;
                User u = uOpt.get();
                Long mid = u.getMasterId();
                if (mid != null) return mid;
                // Fallback: попробуем взять masterId из любой привязанной бренда
                try {
                    if (u.getBrands() != null && !u.getBrands().isEmpty()) {
                        var any = u.getBrands().iterator().next();
                        if (any != null && any.getMaster() != null && any.getMaster().getId() != null) {
                            return any.getMaster().getId();
                        }
                    }
                } catch (Exception ignored) {
                }
                return null;
            }
            return null; // для остальных ролей
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
