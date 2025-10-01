package kirillzhdanov.identityservice.security;

import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.tenant.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Простой слой авторизации для BL-2 (RBAC по membership-ролям).
 * Использование: инжектировать в сервис/контроллер и вызывать методы require* перед бизнес-операцией.
 */
@Component
public class RbacGuard {

    private RoleMembership roleOrThrow() {
        return TenantContext.getRoleOrThrow();
    }

    public void requireOwnerOrAdmin() {
        RoleMembership r = roleOrThrow();
        if (!(r == RoleMembership.OWNER || r == RoleMembership.ADMIN)) {
            throw new AccessDeniedException("Недостаточно прав: требуется OWNER или ADMIN");
        }
    }

    public void requireOwner() {
        RoleMembership r = roleOrThrow();
        if (r != RoleMembership.OWNER) {
            throw new AccessDeniedException("Недостаточно прав: требуется OWNER");
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
