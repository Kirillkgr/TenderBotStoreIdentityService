package kirillzhdanov.identityservice.config;

import jakarta.annotation.PostConstruct;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.model.master.UserMembership;
import kirillzhdanov.identityservice.model.pickup.PickupPoint;
import kirillzhdanov.identityservice.model.product.Product;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.ProductRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import kirillzhdanov.identityservice.repository.pickup.PickupPointRepository;
import kirillzhdanov.identityservice.service.RoleService;
import kirillzhdanov.identityservice.service.impl.newImpl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * DatabaseInitializer
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed.demoData", havingValue = "true", matchIfMissing = false)
public class DatabaseInitializer {

    private final RoleService roleService;

    private final UserServiceImpl userRepository;

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final PickupPointRepository pickupPointRepository;
    private final MasterAccountRepository masterAccountRepository;
    private final UserMembershipRepository userMembershipRepository;

    public DatabaseInitializer(RoleService roleService, UserServiceImpl userRepository, BrandRepository brandRepository,
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
            // Формат: {name, description, price, promoPrice(optional or empty)} — цены в рублях, реалистичные
            List<String[]> items = List.of(
                    new String[]{"Кофе Американо", "Объем 250 мл. Классический черный кофе средней крепости, готовится методом пролива эспрессо через горячую воду. Вкус чистый и сбалансированный, подходит для повседневного употребления; отлично сочетается с десертами и выпечкой.", "180.00", ""},
                    new String[]{"Капучино", "Объем 300 мл. Эспрессо с подогретым молоком 3.2% и плотной молочной пеной. Классическое соотношение 1:1:1 (эспрессо/молоко/пена). Ноты карамели и ореха, умеренная сладость. По просьбе гостя готовим на безлактозном или растительном молоке (овсяное/миндальное).", "250.00", "229.00"},
                    new String[]{"Латте", "Объем 300 мл. Деликатный кофейный напиток с большим количеством молока и тонким слоем пены. Мягкий вкус без горечи, идеален для тех, кто предпочитает более сливочные и нежные напитки. Возможно добавить сироп (ваниль, карамель, фундук).", "280.00", ""},
                    new String[]{"Эспрессо", "Объем 60 мл (двойной шот). Концентрированный напиток с плотным телом и насыщенным ароматом. Идеален для быстрого бодрящего эффекта. Подается в разогретой чашке, лучше всего раскрывается без сахара.", "150.00", ""},
                    new String[]{"Чизкейк Нью-Йорк", "Порция 120 г. Классический запеченный чизкейк на хрустящей песочной основе. Нежная текстура, умеренная сладость, подается охлажденным. Отлично сочетается с черным кофе или латте.", "350.00", ""},
                    new String[]{"Круассан сливочный", "Вес 70 г. Слоеная выпечка на сливочном масле с легкой хрустящей корочкой и мягким слоем внутри. Идеален к утреннему кофе; можно подогреть по запросу.", "120.00", ""}
            );
            int created = 0;
            for (String[] it : items) {
                String name = it[0];
                List<Product> rootProducts = productRepository.findByBrandAndGroupTagIsNull(b);
                Optional<Product> existingOpt = rootProducts.stream()
                        .filter(pr -> name.equalsIgnoreCase(pr.getName()))
                        .findFirst();
                if (existingOpt.isEmpty()) {
                    Product p = new Product();
                    p.setName(name);
                    p.setDescription(it[1]);
                    p.setPrice(new BigDecimal(it[2]));
                    if (it.length > 3 && it[3] != null && !it[3].isBlank()) {
                        p.setPromoPrice(new BigDecimal(it[3]));
                    }
                    p.setBrand(b);
                    p.setVisible(true);
                    productRepository.save(p);
                    created++;
                } else {
                    // Update existing product if data differs (description/price/promo)
                    Product p = existingOpt.get();
                    boolean changed = false;
                    String newDesc = it[1];
                    BigDecimal newPrice = new BigDecimal(it[2]);
                    BigDecimal newPromo = (it.length > 3 && it[3] != null && !it[3].isBlank()) ? new BigDecimal(it[3]) : null;
                    if (!Objects.equals(p.getDescription(), newDesc)) {
                        p.setDescription(newDesc);
                        changed = true;
                    }
                    if (p.getPrice() == null || p.getPrice().compareTo(newPrice) != 0) {
                        p.setPrice(newPrice);
                        changed = true;
                    }
                    if (!Objects.equals(p.getPromoPrice(), newPromo)) {
                        p.setPromoPrice(newPromo);
                        changed = true;
                    }
                    if (changed) {
                        productRepository.save(p);
                        log.info("Updated seeded product '{}' for brand {}", p.getName(), b.getName());
                    }
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
                user = userRepository.findByUsername(username).orElse(null);
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
                // Назначим роль membership по кругу для разнообразия
                RoleMembership memRole;
                int mod = (idx + i) % 5;
                if (mod == 0) memRole = RoleMembership.OWNER;
                else if (mod == 1) memRole = RoleMembership.ADMIN;
                else if (mod == 2) memRole = RoleMembership.CASHIER;
                else if (mod == 3) memRole = RoleMembership.COOK;
                else memRole = RoleMembership.CLIENT;
                try {
                    um.setRole(memRole);
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

        // Явные демо-пользователи с фиксированными ролями для ручной проверки
        createOrUpdateDemoUser("demo_owner", Set.of(Role.RoleName.OWNER), brands);
        createOrUpdateDemoUser("demo_admin", Set.of(Role.RoleName.ADMIN), brands);
        createOrUpdateDemoUser("demo_user", Set.of(Role.RoleName.USER), brands);
        // demo accounts with specific membership roles for manual guard checks
        createOrUpdateDemoMembershipUser("demo_cook", RoleMembership.COOK, brands);
        createOrUpdateDemoMembershipUser("demo_cashier", RoleMembership.CASHIER, brands);
        createOrUpdateDemoMembershipUser("demo_client", RoleMembership.CLIENT, brands);
    }

    private void createRoleIfNotExists(Role.RoleName roleName) {

        roleService.createRoleIfNotExists(roleName);
    }

    private void createOrUpdateDemoUser(String username, Set<Role.RoleName> roleNames, List<Brand> brands) {
        boolean exists = userRepository.existsByUsername(username);
        Map<Role.RoleName, Role> roleMap = new HashMap<>();
        for (Role.RoleName rn : Role.RoleName.values()) {
            roleService.findByName(rn).ifPresent(r -> roleMap.put(rn, r));
        }
        Set<Role> roles = new HashSet<>();
        for (Role.RoleName rn : roleNames) {
            Role r = roleMap.get(rn);
            if (r != null) roles.add(r);
        }
        if (!exists) {
            User u = userRepository.saveNewUser(User.builder()
                    .username(username)
                    .password(username)
                    .build());
            u.setRoles(roles);
            // Привязываем хотя бы один бренд для наглядности
            if (!brands.isEmpty()) {
                u.setBrands(Set.of(brands.getFirst()));
            }
            userRepository.save(u);
            log.info("Created demo user {} with roles {}", username, roles.stream().map(r -> r.getName().name()).toList());
        }
    }

    private void createOrUpdateDemoMembershipUser(String username, RoleMembership memRole, List<Brand> brands) {
        boolean exists = userRepository.existsByUsername(username);
        User u;
        if (!exists) {
            u = userRepository.saveNewUser(User.builder()
                    .username(username)
                    .password(username)
                    .build());
            userRepository.save(u);
        } else {
            u = userRepository.findByUsername(username).orElse(null);
        }
        // ensure at least one membership with the requested role
        if (brands.isEmpty()) return;
        Brand brand = brands.getFirst();
        MasterAccount master = masterAccountRepository.findByName("M1").orElseGet(() ->
                masterAccountRepository.save(MasterAccount.builder().name("M1").status("ACTIVE").build())
        );
        boolean hasRole = userMembershipRepository.findByUserId(u.getId())
                .stream().anyMatch(m -> {
                    try {
                        return m.getRole() == memRole;
                    } catch (Exception e) {
                        return false;
                    }
                });
        if (!hasRole) {
            UserMembership um = new UserMembership();
            um.setUser(u);
            um.setMaster(master);
            try {
                um.setBrand(brand);
            } catch (Exception ignored) {
            }
            try {
                List<PickupPoint> act = pickupPointRepository.findByBrand_IdAndActiveTrue(brand.getId());
                if (!act.isEmpty()) um.setPickupPoint(act.getFirst());
            } catch (Exception ignored) {
            }
            try {
                um.setRole(memRole);
            } catch (Exception ignored) {
            }
            userMembershipRepository.save(um);
            log.info("Created demo membership for {} with role {}", username, memRole);
        }
    }
}
