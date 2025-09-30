package kirillzhdanov.identityservice.config;

import jakarta.annotation.PostConstruct;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.model.master.UserMembership;
import kirillzhdanov.identityservice.model.pickup.PickupPoint;
import kirillzhdanov.identityservice.model.product.Product;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.ProductRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
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
    private final MasterAccountRepository masterAccountRepository;
    private final UserMembershipRepository userMembershipRepository;

    public DatabaseInitializer(RoleService roleService, UserService userRepository, BrandRepository brandRepository,
                               ProductRepository productRepository, PickupPointRepository pickupPointRepository,
                               MasterAccountRepository masterAccountRepository, UserMembershipRepository userMembershipRepository) {

        this.roleService = roleService;
        this.userRepository = userRepository;
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.pickupPointRepository = pickupPointRepository;
        this.masterAccountRepository = masterAccountRepository;
        this.userMembershipRepository = userMembershipRepository;
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

        // Добавить тестовые товары и точки самовывоза для каждого бренда (идемпотентно)
        for (Brand b : brands) {
            // Товары по бренду (если их нет — создадим базовый набор)
            List<String[]> items = List.of(
                    new String[]{"Кофе Американо", "Классический американо 250мл", "2.50"},
                    new String[]{"Капучино", "Капучино 300мл", "3.20"},
                    new String[]{"Латте", "Латте 300мл", "3.40"},
                    new String[]{"Чизкейк", "Нью-Йорк, порция 120г", "4.10"},
                    new String[]{"Круассан", "Сливочный, 70г", "1.80"}
            );
            int created = 0;
            for (String[] it : items) {
                String name = it[0];
                boolean exists = productRepository.findByBrandAndGroupTagIsNull(b)
                        .stream()
                        .anyMatch(pr -> name.equalsIgnoreCase(pr.getName()));
                if (!exists) {
                    Product p = new Product();
                    p.setName(name);
                    p.setDescription(it[1]);
                    p.setPrice(new BigDecimal(it[2]));
                    p.setBrand(b);
                    p.setVisible(true);
                    productRepository.save(p);
                    created++;
                }
            }
            if (created > 0) {
                log.info("Seeded {} products for brand {}", created, b.getName());
            }

            // Точки самовывоза: стремимся иметь минимум 2 активные точки на бренд
            List<PickupPoint> active = pickupPointRepository.findByBrand_IdAndActiveTrue(b.getId());
            int need = Math.max(0, 2 - active.size());
            for (int k = 1; k <= need; k++) {
                String name = b.getName() + " Точка №" + (active.size() + k);
                PickupPoint pp = PickupPoint.builder()
                        .brand(b)
                        .name(name)
                        .address("г. Тестоград, ул. Примерная, д. " + (10 + k))
                        .active(true)
                        .build();
                pickupPointRepository.save(pp);
                log.info("Seeded pickup point '{}' for brand {}", name, b.getName());
            }
        }

        // Создание/обновление пользователей и обеспечение наличия memberships
        for (int i = 1; i <= 5; i++) {
            String username = "user" + i;

            boolean exists = userRepository.existsByUsername(username);
            User user;
            Set<Brand> userBrands = new HashSet<>();

            if (!exists) {
                // Получить все роли
                Map<Role.RoleName, Role> roleMap = new HashMap<>();
                for (Role.RoleName rn : Role.RoleName.values()) {
                    roleService.findByName(rn)
                            .ifPresent(r -> roleMap.put(rn, r));
                }
                Set<Role> roles = new HashSet<>();
                // Назначить роли по кругу
                if (i % 3 == 1) roles.add(roleMap.get(Role.RoleName.USER));
                if (i % 3 == 2) roles.add(roleMap.get(Role.RoleName.ADMIN));
                if (i % 3 == 0) roles.add(roleMap.get(Role.RoleName.OWNER));

                // Привязать несколько брендов (для новых пользователей)
                userBrands.add(brands.get(i % brands.size()));
                if (i % 2 == 0) userBrands.add(brands.get((i + 1) % brands.size()));

                user = userRepository.saveNewUser(User.builder()
                        .username(username)
                        .password("user" + i)
                        .build());
                user.setBrands(userBrands);
                user.setRoles(roles);
                userRepository.save(user);
                log.info("Created user: {} with roles: {} and brands: {}", username, roles.stream()
                        .map(r -> r.getName().name())
                        .toList(), userBrands.stream().map(Brand::getName).toList());
            } else {
                // Пользователь уже есть — получим его и его бренды
                user = userRepository.findByUsername(username);
                try {
                    if (user.getBrands() != null) userBrands.addAll(user.getBrands());
                } catch (Exception ignored) {
                }
                if (userBrands.isEmpty() && !brands.isEmpty()) {
                    userBrands.add(brands.getFirst());
                }
            }

            // Создать MasterAccount(ы) и UserMembership(ы) для пользователя, если ещё нет
            // Минимум 1 membership для нечётных, 2 для чётных пользователей
            int desiredMemberships = (i % 2 == 0) ? 2 : 1;
            List<UserMembership> existing = userMembershipRepository.findByUserId(user.getId());
            if (existing.size() >= desiredMemberships) continue;

            // Гарантируем наличие 2 master-аккаунтов M1/M2
            MasterAccount m1 = masterAccountRepository.findByName("M1").orElseGet(() ->
                    masterAccountRepository.save(MasterAccount.builder().name("M1").status("ACTIVE").build())
            );
            MasterAccount m2 = masterAccountRepository.findByName("M2").orElseGet(() ->
                    masterAccountRepository.save(MasterAccount.builder().name("M2").status("ACTIVE").build())
            );

            List<Brand> brandList = new ArrayList<>(userBrands);
            if (brandList.isEmpty() && !brands.isEmpty()) brandList.add(brands.getFirst());

            // Привяжем 1-2 membership: к разным мастерам и брендам по возможности
            List<MasterAccount> masters = List.of(m1, m2);
            int createdMems = 0;
            for (int idx = 0; idx < desiredMemberships; idx++) {
                MasterAccount master = masters.get(idx % masters.size());
                Brand brand = brandList.get(idx % brandList.size());

                boolean existsMem = existing.stream().anyMatch(um ->
                        um.getMaster() != null && um.getMaster().getId().equals(master.getId())
                );
                if (existsMem) continue;

                UserMembership um = new UserMembership();
                um.setUser(user);
                um.setMaster(master);
                // brand/location опциональны — зададим brand, если модель поддерживает
                try {
                    um.setBrand(brand);
                } catch (Exception ignored) {
                }
                // Выберем первую активную точку бренда, если есть
                try {
                    List<PickupPoint> act = pickupPointRepository.findByBrand_IdAndActiveTrue(brand.getId());
                    if (!act.isEmpty()) {
                        try {
                            um.setPickupPoint(act.getFirst());
                        } catch (Exception ignored2) {
                        }
                    }
                } catch (Exception ignored) {
                }

                userMembershipRepository.save(um);
                createdMems++;
            }
            if (createdMems > 0) {
                log.info("Seeded {} memberships for user {}", createdMems, username);
            }
        }
    }

    private void createRoleIfNotExists(Role.RoleName roleName) {

        roleService.createRoleIfNotExists(roleName);
    }
}
