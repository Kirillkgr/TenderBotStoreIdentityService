package kirillzhdanov.identityservice.tenant;

import kirillzhdanov.identityservice.model.master.RoleMembership;

public final class TenantContext {
    private static final ThreadLocal<Long> MASTER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> MEMBERSHIP_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> BRAND_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> LOCATION_ID = new ThreadLocal<>();
    private static final ThreadLocal<RoleMembership> ROLE = new ThreadLocal<>();

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
        MEMBERSHIP_ID.remove();
        BRAND_ID.remove();
        LOCATION_ID.remove();
        ROLE.remove();
    }

    public static Long getMembershipId() {
        return MEMBERSHIP_ID.get();
    }

    public static void setMembershipId(Long membershipId) {
        MEMBERSHIP_ID.set(membershipId);
    }

    public static Long getBrandId() {
        return BRAND_ID.get();
    }

    public static void setBrandId(Long brandId) {
        BRAND_ID.set(brandId);
    }

    public static Long getLocationId() {
        return LOCATION_ID.get();
    }

    public static void setLocationId(Long locationId) {
        LOCATION_ID.set(locationId);
    }

    public static RoleMembership getRole() {
        return ROLE.get();
    }

    public static void setRole(RoleMembership role) {
        ROLE.set(role);
    }

    public static RoleMembership getRoleOrThrow() {
        RoleMembership r = ROLE.get();
        if (r == null) throw new IllegalStateException("Role context is not set");
        return r;
    }
}
