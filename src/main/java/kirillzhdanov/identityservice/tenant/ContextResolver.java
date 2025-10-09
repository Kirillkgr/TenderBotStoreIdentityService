package kirillzhdanov.identityservice.tenant;

/**
 * Управление контекстом "сеанса" для одного HTTP-запроса.
 * <p>
 * Здесь хранится информация о выбранных пользователем сущностях:
 * - идентификатор мастер-аккаунта (masterId)
 * - идентификатор бренда (brandId)
 * - идентификатор пункта самовывоза (pickupPointId)
 * - время выпуска контекста (issuedAt), чтобы понимать, когда он был обновлён.
 * <p>
 * Контекст извлекается из HttpOnly cookie (напр. фильтром CtxCookieFilter)
 * и помещается в ThreadLocal на время обработки запроса. После завершения
 * запроса контекст ОБЯЗАТЕЛЬНО очищается.
 */
public final class ContextResolver {
    private static final ThreadLocal<Ctx> CTX = new ThreadLocal<>();

    private ContextResolver() {
    }

    /**
     * Устанавливает контекст для текущего запроса. Вызывать из инфраструктурного кода
     * (например, фильтра), после проверки подписи и корректности данных в cookie.
     */
    public static void set(Ctx ctx) {
        CTX.set(ctx);
    }

    /**
     * Возвращает текущий контекст или null, если контекст не установлен.
     */
    public static Ctx current() {
        return CTX.get();
    }

    /**
     * Очищает контекст после завершения обработки запроса. Вызывать в finally.
     */
    public static void clear() {
        CTX.remove();
    }

    /**
     * Носитель значений контекста. Иммутабельный объект.
     */
    public static final class Ctx {
        private final Long masterId;
        private final Long brandId;
        private final Long pickupPointId;
        private final Long issuedAt;

        public Ctx(Long masterId, Long brandId, Long pickupPointId, Long issuedAt) {
            this.masterId = masterId;
            this.brandId = brandId;
            this.pickupPointId = pickupPointId;
            this.issuedAt = issuedAt;
        }

        /**
         * Идентификатор мастер-аккаунта ("владелец" брендов).
         */
        public Long masterId() {
            return masterId;
        }

        /**
         * Идентификатор бренда, в контексте которого работает пользователь.
         */
        public Long brandId() {
            return brandId;
        }

        /**
         * Идентификатор выбранного пункта самовывоза (если применимо).
         */
        public Long pickupPointId() {
            return pickupPointId;
        }

        /**
         * Момент времени (epoch millis), когда контекст был установлен/обновлён.
         */
        public Long issuedAt() {
            return issuedAt;
        }
    }
}
