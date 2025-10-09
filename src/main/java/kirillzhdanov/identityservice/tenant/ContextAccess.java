package kirillzhdanov.identityservice.tenant;

/**
 * Утилитный класс для безопасного доступа к значениям контекста из бизнес-кода.
 * <p>
 * Источником контекста является {@link ContextResolver}, который наполняется из HttpOnly cookie
 * (см. {@code CtxCookieFilter}). Для плавной миграции остаются фоллбеки на {@link TenantContext},
 * которые будут удалены после полного отказа от заголовков X-*.
 */
public final class ContextAccess {
    private ContextAccess() {
    }

    /**
     * Возвращает идентификатор мастер-аккаунта из контекста или null, если он не установлен.
     * При миграции возможен возврат значения из {@link TenantContext} (legacy).
     */
    public static Long getMasterIdOrNull() {
        ContextResolver.Ctx c = ContextResolver.current();
        if (c != null && c.masterId() != null) return c.masterId();
        return TenantContext.getMasterId(); // legacy fallback during migration
    }

    /**
     * Возвращает идентификатор бренда из контекста или null.
     * При миграции возможен возврат значения из {@link TenantContext} (legacy).
     */
    public static Long getBrandIdOrNull() {
        ContextResolver.Ctx c = ContextResolver.current();
        if (c != null && c.brandId() != null) return c.brandId();
        return TenantContext.getBrandId();
    }

    /**
     * Возвращает идентификатор пункта самовывоза из контекста или null.
     * При миграции возможен возврат значения из {@link TenantContext} (legacy).
     */
    public static Long getPickupPointIdOrNull() {
        ContextResolver.Ctx c = ContextResolver.current();
        if (c != null && c.pickupPointId() != null) return c.pickupPointId();
        return TenantContext.getLocationId();
    }

    /**
     * Возвращает masterId или выбрасывает исключение, если контекст не установлен.
     * Удобно использовать там, где masterId обязателен по бизнес-правилам.
     */
    public static Long getMasterIdOrThrow() {
        Long id = getMasterIdOrNull();
        if (id == null) throw new IllegalStateException("Context masterId is not set");
        return id;
    }
}
