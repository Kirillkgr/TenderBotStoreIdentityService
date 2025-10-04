package kirillzhdanov.identityservice.controller.admin;

import kirillzhdanov.identityservice.dto.order.OrderDto;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import kirillzhdanov.identityservice.security.RbacGuard;
import kirillzhdanov.identityservice.service.admin.OrderAdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/v1/orders")
@PreAuthorize("hasAnyRole('ADMIN','OWNER')")
public class AdminOrderController {

    private final OrderAdminService orderAdminService;
    private final RbacGuard rbacGuard;
    private final UserRepository userRepository;
    private final UserMembershipRepository membershipRepository;

    public AdminOrderController(OrderAdminService orderAdminService, RbacGuard rbacGuard,
                                UserRepository userRepository, UserMembershipRepository membershipRepository) {
        this.orderAdminService = orderAdminService;
        this.rbacGuard = rbacGuard;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @GetMapping
    public ResponseEntity<Page<OrderDto>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestHeader(value = "X-Master-Id", required = false) Long headerMasterId
    ) {
        // Enforce membership-based RBAC: only OWNER/ADMIN in current tenant context
        rbacGuard.requireOwnerOrAdmin();

        // Require X-Master-Id: 0 (мастер-пользователь) или masterId, к которому у пользователя есть membership
        if (headerMasterId == null) {
            throw new AccessDeniedException("X-Master-Id header is required");
        }
        if (headerMasterId != 0L) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                throw new AccessDeniedException("Not authenticated");
            }
            String username = auth.getName();
            Long userId = userRepository.findByUsername(username)
                    .orElseThrow(() -> new AccessDeniedException("User not found"))
                    .getId();
            boolean hasMembership = membershipRepository.findByUserIdAndMasterId(userId, headerMasterId).isPresent();
            if (!hasMembership) {
                throw new AccessDeniedException("Invalid X-Master-Id");
            }
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<OrderDto> result = orderAdminService.findOrders(pageable, search, brandId, dateFrom, dateTo);
        return ResponseEntity.ok(result);
    }
}
