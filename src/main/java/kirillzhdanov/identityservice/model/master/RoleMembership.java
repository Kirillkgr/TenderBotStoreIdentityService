package kirillzhdanov.identityservice.model.master;

/**
 * RBAC роли на уровне Membership.
 */
public enum RoleMembership {
    OWNER,
    ADMIN,
    CASHIER,
    COOK,
    CLIENT;

    // Базовые предикаты для простых проверок (B2-BK-2)
    public boolean isOwnerOrAdmin() {
        return this == OWNER || this == ADMIN;
    }

    public boolean canManageCatalog() {
        return isOwnerOrAdmin();
    }

    public boolean isCashier() {
        return this == CASHIER;
    }

    public boolean isCook() {
        return this == COOK;
    }

    public boolean isClient() {
        return this == CLIENT;
    }
}
