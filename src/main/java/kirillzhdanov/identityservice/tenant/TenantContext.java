package kirillzhdanov.identityservice.tenant;

public final class TenantContext {
    private static final ThreadLocal<Long> MASTER_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Long getMasterId() {
        return MASTER_ID.get();
    }

    public static void setMasterId(Long masterId) {
        MASTER_ID.set(masterId);
    }

    public static Long getMasterIdOrThrow() {
        Long id = MASTER_ID.get();
        if (id == null) throw new IllegalStateException("Master context is not set");
        return id;
    }

    public static void clear() {
        MASTER_ID.remove();
    }
}
