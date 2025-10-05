package kirillzhdanov.identityservice.security;

import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.tenant.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Простой слой авторизации для BL-2 (RBAC по membership-ролям).
 * Использование: инжектировать в сервис/контроллер и вызывать методы require* перед бизнес-операцией.
 */
@Component
public class RbacGuard {

    private RoleMembership roleOrThrow() {
        RoleMembership role = TenantContext.getRole();
        if (role == null) {
            // Вместо IllegalStateException кидаем AccessDenied -> 403, а не 500
            throw new AccessDeniedException("Контекст роли не установлен");
        }
        return role;
    }

    public void requireOwner() {
        RoleMembership r = roleOrThrow();
        if (r != RoleMembership.OWNER) {
            throw new AccessDeniedException("Недостаточно прав: требуется OWNER");
        }
    }

    public void requireOwnerOrAdmin() {
        RoleMembership r = roleOrThrow();
        if (!(r == RoleMembership.OWNER || r == RoleMembership.ADMIN)) {
            throw new AccessDeniedException("Недостаточно прав: требуется OWNER или ADMIN");
        }
    }

    /**
     * Требует, чтобы пользователь был аутентифицирован.
     */
    public void requireAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Требуется аутентификация");
        }
    }

    /**
     * Глобальная проверка ролей из JWT/Authentication, независимо от tenant membership.
     * Требуются ROLE_OWNER или ROLE_ADMIN среди GrantedAuthority.
     */
    public void requireOwnerOrAdminGlobal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Требуется аутентификация");
        }
        boolean ok = false;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String a = ga.getAuthority();
            if ("ROLE_OWNER".equals(a) || "ROLE_ADMIN".equals(a)) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            throw new AccessDeniedException("Недостаточно прав: требуется OWNER или ADMIN (глобально)");
        }
    }

    public void requireCashier() {
        RoleMembership r = roleOrThrow();
        if (r != RoleMembership.CASHIER) {
            throw new AccessDeniedException("Недостаточно прав: требуется CASHIER");
        }
    }

    public void requireCook() {
        RoleMembership r = roleOrThrow();
        if (r != RoleMembership.COOK) {
            throw new AccessDeniedException("Недостаточно прав: требуется COOK");
        }
    }

    public void requireClientOrAbove() {
        RoleMembership r = roleOrThrow();
        // Любая роль допустима, но контекст должен быть установлен
        if (r == null) {
            throw new AccessDeniedException("Контекст роли не установлен");
        }
    }

    // Удобный метод для операций каталога (создание/обновление/удаление)
    public void requireCanManageCatalog() {
        RoleMembership r = roleOrThrow();
        if (!(r == RoleMembership.OWNER || r == RoleMembership.ADMIN)) {
            throw new AccessDeniedException("Недостаточно прав для управления каталогом (нужно OWNER/ADMIN)");
        }
    }
}
