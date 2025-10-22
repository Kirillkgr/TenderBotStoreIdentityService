package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.exception.BadRequestException;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.model.master.UserMembership;
import kirillzhdanov.identityservice.model.pickup.PickupPoint;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import kirillzhdanov.identityservice.repository.pickup.PickupPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * Shared provisioning logic for new users (classic registration and OAuth2).
 */
@Service
@RequiredArgsConstructor
public class ProvisioningService implements ProvisioningServiceOps {

    private final MasterAccountRepository masterAccountRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final BrandRepository brandRepository;
    private final PickupPointRepository pickupPointRepository;
    private final UserService userService;

    /**
     * Ensure there is a MasterAccount for the user.
     */
    @Transactional
    public MasterAccount ensureMasterAccountForUser(User user) {
        try {
            return masterAccountRepository.findByName(user.getUsername())
                    .orElseGet(() -> masterAccountRepository.save(
                            MasterAccount.builder()
                                    .name(user.getUsername())
                                    .status("ACTIVE")
                                    .build()
                    ));
        } catch (Exception e) {
            return masterAccountRepository.findByName(user.getUsername())
                    .orElseThrow(() -> new BadRequestException("Unable to ensure master account"));
        }
    }

    /**
     * Ensure OWNER membership for the user within the given master account.
     */
    @Transactional
    public void ensureOwnerMembership(User user, MasterAccount master) {
        if (master == null) return;
        final Long masterId = master.getId();
        boolean hasMembership = false;
        for (UserMembership m : userMembershipRepository.findByUserId(user.getId())) {
            if (m.getMaster() != null && Objects.equals(m.getMaster().getId(), masterId)) {
                hasMembership = true;
                break;
            }
        }
        if (!hasMembership) {
            UserMembership um = UserMembership.builder()
                    .user(user)
                    .master(master)
                    .role(RoleMembership.OWNER)
                    .status("ACTIVE")
                    .build();
            userMembershipRepository.save(um);
        }
    }

    /**
     * Create default brand and pickup point, link to user and membership.
     */
    @Transactional
    public void ensureDefaultBrandAndPickup(User user, MasterAccount master) {
        if (master == null) return;

        // Generate a unique brand name slug
        String defaultBrandName = generateSlugFromUUID();
        while (brandRepository.existsByNameAndMaster_Id(defaultBrandName, master.getId())) {
            defaultBrandName = generateSlugFromUUID();
        }

        Brand defaultBrand = Brand.builder()
                .name(defaultBrandName)
                .organizationName(defaultBrandName)
                .master(master)
                .build();
        defaultBrand = brandRepository.saveAndFlush(defaultBrand);

        PickupPoint defaultPickup = PickupPoint.builder()
                .brand(defaultBrand)
                .name("Основная точка")
                .active(true)
                .build();
        pickupPointRepository.saveAndFlush(defaultPickup);

        user.getBrands().add(defaultBrand);
        userService.save(user);

        // Update existing membership without brand, or create new with brand
        UserMembership membershipWithoutBrand = null;
        for (UserMembership m : userMembershipRepository.findByUserId(user.getId())) {
            if (m.getMaster() != null && Objects.equals(m.getMaster().getId(), master.getId()) && m.getBrand() == null) {
                membershipWithoutBrand = m;
                break;
            }
        }

        if (membershipWithoutBrand != null) {
            membershipWithoutBrand.setBrand(defaultBrand);
            if (membershipWithoutBrand.getRole() == null) {
                try { membershipWithoutBrand.setRole(RoleMembership.OWNER); } catch (Exception ignored) {}
            }
            if (membershipWithoutBrand.getStatus() == null) {
                membershipWithoutBrand.setStatus("ACTIVE");
            }
            userMembershipRepository.save(membershipWithoutBrand);
        } else {
            UserMembership brandMembership = UserMembership.builder()
                    .user(user)
                    .master(master)
                    .brand(defaultBrand)
                    .role(RoleMembership.OWNER)
                    .status("ACTIVE")
                    .build();
            userMembershipRepository.save(brandMembership);
        }
    }

    private String generateSlugFromUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
    }
}
