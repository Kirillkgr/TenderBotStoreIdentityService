package kirillzhdanov.identityservice.config;

import jakarta.annotation.PostConstruct;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.pickup.PickupPoint;
import kirillzhdanov.identityservice.model.product.Product;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.ProductRepository;
import kirillzhdanov.identityservice.repository.pickup.PickupPointRepository;
import kirillzhdanov.identityservice.service.RoleService;
import kirillzhdanov.identityservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * DatabaseInitializer
 */
@Slf4j
@Component
public class DatabaseInitializer {

    private final RoleService roleService;

    private final UserService userRepository;

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final PickupPointRepository pickupPointRepository;

    public DatabaseInitializer(RoleService roleService, UserService userRepository, BrandRepository brandRepository,
                               ProductRepository productRepository, PickupPointRepository pickupPointRepository) {

        this.roleService = roleService;
        this.userRepository = userRepository;
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.pickupPointRepository = pickupPointRepository;
    }

    @PostConstruct
    @Transactional
    public void init() {
        // Создание ролей
        createRoleIfNotExists(Role.RoleName.USER);
        createRoleIfNotExists(Role.RoleName.ADMIN);
        createRoleIfNotExists(Role.RoleName.OWNER);
        log.info("Database initialized with default roles");

        // Создание брендов
        List<Brand> brands = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            String brandName = "Brand" + i;
            String token = "token" + i;
            Brand brand = brandRepository.existsByName(brandName) ? brandRepository.findAll()
                    .stream()
                    .filter(b -> b.getName()
                            .equals(brandName))
                    .findFirst()
                    .orElse(null) : brandRepository.save(Brand.builder()
                    .name(brandName)
                    .telegramBotToken(token)
                    .build());
            brands.add(brand);
        }
        log.info("Brands: {}", brands.stream()
                .map(Brand::getName)
                .toList());

        // Добавить тестовый товар и точку самовывоза для первого бренда
        if (!brands.isEmpty()) {
            Brand first = brands.get(0);
            boolean hasAnyProduct = !productRepository.findByBrandAndGroupTagIsNull(first).isEmpty();
            if (!hasAnyProduct) {
                Product p = new Product();
                p.setName("Кофе Американо");
                p.setDescription("Классический американо 250мл");
                p.setPrice(new BigDecimal("2.50"));
                p.setBrand(first);
                p.setVisible(true);
                productRepository.save(p);
                log.info("Seeded sample product '{}' for brand {}", p.getName(), first.getName());
            }
            boolean hasPickup = !pickupPointRepository.findByBrand_IdAndActiveTrue(first.getId()).isEmpty();
            if (!hasPickup) {
                PickupPoint pp = PickupPoint.builder()
                        .brand(first)
                        .name(first.getName() + " Точка №1")
                        .address("г. Тестоград, ул. Примерная, д. 1")
                        .active(true)
                        .build();
                pickupPointRepository.save(pp);
                log.info("Seeded sample pickup point '{}' for brand {}", pp.getName(), first.getName());
            }
        }

        // Создание пользователей с разными ролями и брендами
        for (int i = 1; i <= 5; i++) {
            String username = "user" + i;
            if (userRepository.existsByUsername(username))
                continue;
            // Получить все роли
            Map<Role.RoleName, Role> roleMap = new HashMap<>();
            for (Role.RoleName rn : Role.RoleName.values()) {
                roleService.findByName(rn)
                        .ifPresent(r -> roleMap.put(rn, r));
            }
            Set<Role> roles = new HashSet<>();
            // Назначить роли по кругу
            if (i % 3 == 1)
                roles.add(roleMap.get(Role.RoleName.USER));
            if (i % 3 == 2)
                roles.add(roleMap.get(Role.RoleName.ADMIN));
            if (i % 3 == 0)
                roles.add(roleMap.get(Role.RoleName.OWNER));

            // Привязать несколько брендов
            Set<Brand> userBrands = new HashSet<>();
            userBrands.add(brands.get(i % brands.size()));
            if (i % 2 == 0)
                userBrands.add(brands.get((i + 1) % brands.size()));
            User user = userRepository.saveNewUser(User.builder()
                    .username(username)
                    .password("user" + i)
                    .build());
            user.setBrands(userBrands);
            user.setRoles(roles);
            userRepository.save(user);
            log.info("Created user: {} with roles: {} and brands: {}", username, roles.stream()
                    .map(r -> r.getName()
                            .name())
                    .toList(), userBrands.stream()
                    .map(Brand::getName)
                    .toList());
        }
    }

    private void createRoleIfNotExists(Role.RoleName roleName) {

        roleService.createRoleIfNotExists(roleName);
    }
}
