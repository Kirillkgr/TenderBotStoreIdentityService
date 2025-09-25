package kirillzhdanov.identityservice.util;

public class BrandContextHolder {
    private static final ThreadLocal<Long> BRAND_ID = new ThreadLocal<>();

    public static void set(Long brandId) {
        BRAND_ID.set(brandId);
    }

    public static Long get() {
        return BRAND_ID.get();
    }

    public static void clear() {
        BRAND_ID.remove();
    }
}
