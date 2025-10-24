package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrandLinksReconcileService {

    private final UserMembershipRepository userMembershipRepository;
    private final UserService userService;

    /**
     * Fast self-heal: if user has ACTIVE OWNER/ADMIN memberships with brand, but user.brands
     * misses those brands, add links and save the user.
     */
    @Transactional
    public void reconcileUserBrands(User user) {
        try {
            if (user == null || user.getId() == null) return;
            var memberships = userMembershipRepository.findByUserId(user.getId());
            if (memberships == null || memberships.isEmpty()) return;
            boolean changed = false;
            for (var m : memberships) {
                if (m == null || m.getBrand() == null) continue;
                if (m.getStatus() != null && !"ACTIVE".equalsIgnoreCase(m.getStatus())) continue;
                if (m.getRole() != null && (m.getRole().name().equals("OWNER") || m.getRole().name().equals("ADMIN"))) {
                    if (user.getBrands() != null && !user.getBrands().contains(m.getBrand())) {
                        user.getBrands().add(m.getBrand());
                        changed = true;
                    }
                }
            }
            if (changed) {
                userService.save(user);
            }
        } catch (Exception ignored) {
        }
    }
}
