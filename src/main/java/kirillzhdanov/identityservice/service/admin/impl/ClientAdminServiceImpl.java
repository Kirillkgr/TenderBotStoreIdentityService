package kirillzhdanov.identityservice.service.admin.impl;

import jakarta.persistence.criteria.Predicate;
import kirillzhdanov.identityservice.dto.client.ClientDto;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.service.UserSpecifications;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ClientAdminServiceImpl implements ClientAdminService {

    private final UserRepository userRepository;

    public ClientAdminServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Page<ClientDto> findClients(Pageable pageable, String search, Long masterIdParam) {
        Specification<User> spec = Specification.where(null);

        // Только клиенты (роль USER)
        spec = spec.and(UserSpecifications.byRole("USER"));

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

        // Ограничение для ADMIN по своему masterId
        Long adminMasterId = resolveAdminMasterId();
        if (adminMasterId != null) {
            spec = spec.and(UserSpecifications.byMaster(adminMasterId));
        } else if (masterIdParam != null) {
            // OWNER может фильтровать явно по masterId
            spec = spec.and(UserSpecifications.byMaster(masterIdParam));
        }

        Page<User> page = userRepository.findAll(spec, pageable);
        List<ClientDto> list = new ArrayList<>();
        for (User u : page.getContent()) {
            list.add(ClientDto.builder()
                    .id(u.getId())
                    .firstName(u.getFirstName())
                    .lastName(u.getLastName())
                    .patronymic(u.getPatronymic())
                    .email(u.getEmail())
                    .phone(u.getPhone())
                    .masterId(u.getMasterId())
                    .masterName(null) // можно подтянуть название мастера, если появится сущность/связь
                    .build());
        }
        return new PageImpl<>(list, pageable, page.getTotalElements());
    }

    private Long resolveAdminMasterId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            boolean isOwner = hasRole(auth, "ROLE_OWNER") || hasRole(auth, "OWNER");
            boolean isAdmin = hasRole(auth, "ROLE_ADMIN") || hasRole(auth, "ADMIN");
            if (isOwner) return null; // OWNER без ограничений
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
