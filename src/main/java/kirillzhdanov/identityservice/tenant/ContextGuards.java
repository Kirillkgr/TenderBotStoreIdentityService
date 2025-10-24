package kirillzhdanov.identityservice.tenant;

import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.model.Brand;

/**
 * Centralized helpers to enforce tenant brand context across private operations.
 * Mask mismatches as 404 to avoid leaking existence of foreign resources.
 */
public final class ContextGuards {

    private ContextGuards() {}

    public static void requireBrandInContextOr404(Long brandId) {
        // 1) Primary source: TenantContext (set from JWT claims in JwtAuthenticator)
        Long ctxBrand = TenantContext.getBrandId();
        // 2) Fallback for tests and cookie-based context: ContextResolver (set by CtxCookieFilter)
        if (ctxBrand == null) {
            ContextResolver.Ctx ctx = ContextResolver.current();
            if (ctx != null) ctxBrand = ctx.brandId();
        }
        if (ctxBrand == null || brandId == null || !ctxBrand.equals(brandId)) {
            throw new ResourceNotFoundException("Not found");
        }
    }

    public static void requireEntityBrandMatchesContextOr404(Brand brand) {
        if (brand == null || brand.getId() == null) {
            throw new ResourceNotFoundException("Not found");
        }
        requireBrandInContextOr404(brand.getId());
    }
}
