package kirillzhdanov.identityservice.controller;

import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.staff.*;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/staff/v1")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;
    private final UserRepository userRepository;

    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @GetMapping("/users")
    public ResponseEntity<PagedResponse<UserListItemDto>> listUsers(
            @RequestParam(required = false) Long masterId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    , Authentication auth) {
        Long effectiveMasterId = masterId;
        if (effectiveMasterId == null && auth != null && auth.isAuthenticated()) {
            effectiveMasterId = userRepository.findByUsername(auth.getName()).map(User::getId).orElse(null);
        }
        return ResponseEntity.ok(staffService.listUsers(effectiveMasterId, query, role, departmentId, page, size));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @PostMapping("/users")
    public ResponseEntity<UserListItemDto> createUser(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.ok(staffService.createUser(req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @PutMapping("/users/{id}")
    public ResponseEntity<UserListItemDto> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateStaffUserRequest req,
                                                      Authentication auth) {
        Long masterId = (auth != null && auth.isAuthenticated())
                ? userRepository.findByUsername(auth.getName()).map(User::getId).orElse(null)
                : null;
        return ResponseEntity.ok(staffService.updateUser(id, req, masterId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication auth) {
        Long masterId = (auth != null && auth.isAuthenticated())
                ? userRepository.findByUsername(auth.getName()).map(User::getId).orElse(null)
                : null;
        staffService.deleteUser(id, masterId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @PostMapping("/departments")
    public ResponseEntity<DepartmentDto> createDepartment(@Valid @RequestBody DepartmentCreateRequest req) {
        return ResponseEntity.ok(staffService.createDepartment(req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @GetMapping("/departments")
    public ResponseEntity<java.util.List<DepartmentDto>> listDepartments() {
        return ResponseEntity.ok(staffService.listDepartments());
    }
}
