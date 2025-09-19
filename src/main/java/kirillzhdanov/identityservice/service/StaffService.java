package kirillzhdanov.identityservice.service;

import jakarta.transaction.Transactional;
import kirillzhdanov.identityservice.dto.staff.*;
import kirillzhdanov.identityservice.exception.BadRequestException;
import kirillzhdanov.identityservice.model.Department;
import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.repository.DepartmentRepository;
import kirillzhdanov.identityservice.repository.RoleRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public DepartmentDto createDepartment(DepartmentCreateRequest req) {
        if (departmentRepository.existsByNameIgnoreCase(req.getName())) {
            throw new BadRequestException("Отдел с таким названием уже существует");
        }
        Department d = Department.builder().name(req.getName().trim()).description(req.getDescription()).build();
        d = departmentRepository.save(d);
        log.info("[STAFF] Создан отдел id={} name={}", d.getId(), d.getName());
        return DepartmentDto.builder().id(d.getId()).name(d.getName()).description(d.getDescription()).build();
    }

    @Transactional
    public UserListItemDto createUser(CreateUserRequest req) {
        if (userRepository.existsByUsername(req.getLogin())) {
            throw new BadRequestException("Логин уже используется");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Email уже используется");
        }
        Department department = null;
        if (req.getDepartmentId() != null) {
            department = departmentRepository.findById(req.getDepartmentId())
                    .orElseThrow(() -> new BadRequestException("Отдел не найден"));
        }
        Set<Role> roles = mapRoles(req.getRoles());
        User u = User.builder()
                .username(req.getLogin())
                .password(passwordEncoder.encode(req.getPassword()))
                .lastName(req.getLastName())
                .firstName(req.getFirstName())
                .patronymic(req.getPatronymic())
                .dateOfBirth(req.getBirthDate())
                .email(req.getEmail())
                .phone(req.getPhone())
                .department(department)
                .masterId(req.getMasterId())
                .roles(roles)
                .build();
        u = userRepository.save(u);
        log.info("[STAFF] masterId={} создал пользователя id={} login={}", req.getMasterId(), u.getId(), u.getUsername());
        return toItem(u);
    }

    @Transactional
    public UserListItemDto updateUser(Long id, UpdateStaffUserRequest req, Long masterId) {
        User u = userRepository.findById(id).orElseThrow(() -> new BadRequestException("Пользователь не найден"));
        if (req.getLogin() != null && !req.getLogin().equals(u.getUsername())) {
            if (userRepository.existsByUsername(req.getLogin())) {
                throw new BadRequestException("Логин уже используется");
            }
            u.setUsername(req.getLogin());
        }
        if (req.getEmail() != null && !req.getEmail().equalsIgnoreCase(u.getEmail())) {
            if (userRepository.existsByEmail(req.getEmail())) {
                throw new BadRequestException("Email уже используется");
            }
            u.setEmail(req.getEmail());
        }
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        if (req.getLastName() != null) u.setLastName(req.getLastName());
        if (req.getFirstName() != null) u.setFirstName(req.getFirstName());
        if (req.getPatronymic() != null) u.setPatronymic(req.getPatronymic());
        if (req.getBirthDate() != null) u.setDateOfBirth(req.getBirthDate());
        if (req.getPhone() != null) u.setPhone(req.getPhone());
        if (req.getDepartmentId() != null) {
            Department department = departmentRepository.findById(req.getDepartmentId())
                    .orElseThrow(() -> new BadRequestException("Отдел не найден"));
            u.setDepartment(department);
        }
        if (req.getRoles() != null) {
            u.setRoles(mapRoles(req.getRoles()));
        }
        u = userRepository.save(u);
        log.info("[STAFF] masterId={} изменил пользователя id={} login={}", masterId, u.getId(), u.getUsername());
        return toItem(u);
    }

    @Transactional
    public void deleteUser(Long id, Long masterId) {
        User u = userRepository.findById(id).orElseThrow(() -> new BadRequestException("Пользователь не найден"));
        userRepository.delete(u);
        log.info("[STAFF] masterId={} удалил пользователя id={} login={}", masterId, u.getId(), u.getUsername());
    }

    @Transactional
    public PagedResponse<UserListItemDto> listUsers(Long masterId, String query, String role, Long departmentId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Specification<User> spec = Specification.allOf(
                UserSpecifications.byQuery(query),
                UserSpecifications.byMaster(masterId),
                UserSpecifications.byRole(role),
                UserSpecifications.byDepartment(departmentId)
        );
        Page<User> result = userRepository.findAll(spec, pageable);
        return PagedResponse.<UserListItemDto>builder()
                .items(result.getContent().stream().map(this::toItem).collect(Collectors.toList()))
                .total(result.getTotalElements())
                .page(page)
                .size(size)
                .build();
    }

    private Set<Role> mapRoles(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            throw new BadRequestException("Роли обязательны");
        }
        return codes.stream().map(code -> {
            Role.RoleName rn;
            try { rn = Role.RoleName.valueOf(code.toUpperCase()); } catch (Exception e) { throw new BadRequestException("Неизвестная роль: " + code); }
            return roleRepository.findByName(rn).orElseThrow(() -> new BadRequestException("Роль не найдена: " + rn));
        }).collect(Collectors.toSet());
    }

    private UserListItemDto toItem(User u) {
        return UserListItemDto.builder()
                .id(u.getId())
                .login(u.getUsername())
                .lastName(u.getLastName())
                .firstName(u.getFirstName())
                .patronymic(u.getPatronymic())
                .roles(u.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()))
                .departmentId(u.getDepartment() != null ? u.getDepartment().getId() : null)
                .departmentName(u.getDepartment() != null ? u.getDepartment().getName() : null)
                .createdAt(u.getCreatedAt())
                .build();
    }

    @Transactional
    public java.util.List<DepartmentDto> listDepartments() {
        return departmentRepository.findAll()
                .stream()
                .map(d -> DepartmentDto.builder().id(d.getId()).name(d.getName()).description(d.getDescription()).build())
                .collect(Collectors.toList());
    }
}
