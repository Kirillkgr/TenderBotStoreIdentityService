package kirillzhdanov.identityservice.controller.admin;

import kirillzhdanov.identityservice.dto.order.OrderDto;
import kirillzhdanov.identityservice.security.RbacGuard;
import kirillzhdanov.identityservice.service.admin.OrderAdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/orders")
@PreAuthorize("hasAnyRole('ADMIN','OWNER')")
public class AdminOrderController {

    private final OrderAdminService orderAdminService;
    private final RbacGuard rbacGuard;

    public AdminOrderController(OrderAdminService orderAdminService, RbacGuard rbacGuard) {
        this.orderAdminService = orderAdminService;
        this.rbacGuard = rbacGuard;
    }

    @GetMapping
    public ResponseEntity<Page<OrderDto>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo
    ) {
        // Enforce membership-based RBAC: only OWNER/ADMIN in current tenant context
        rbacGuard.requireOwnerOrAdmin();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<OrderDto> result = orderAdminService.findOrders(pageable, search, brandId, dateFrom, dateTo);
        return ResponseEntity.ok(result);
    }
}
