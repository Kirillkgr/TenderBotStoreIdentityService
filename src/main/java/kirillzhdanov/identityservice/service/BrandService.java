package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.dto.BrandDto;
import kirillzhdanov.identityservice.exception.ResourceAlreadyExistsException;
import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.model.master.UserMembership;
import kirillzhdanov.identityservice.model.pickup.PickupPoint;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import kirillzhdanov.identityservice.repository.pickup.PickupPointRepository;
import kirillzhdanov.identityservice.repository.userbrand.UserBrandMembershipRepository;
import kirillzhdanov.identityservice.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final UserRepository userRepository;
    private final MasterAccountRepository masterAccountRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final PickupPointRepository pickupPointRepository;
    private final UserBrandMembershipRepository userBrandMembershipRepository;
    private final MasterAccountService masterAccountService;

    public List<BrandDto> getAllBrands() {
        Long masterId = TenantContext.getMasterId();
        if (masterId == null) {
            masterId = masterAccountService.resolveOrCreateMasterIdForCurrentUser();
        }
        return brandRepository.findByMaster_Id(masterId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Public-facing list of brands without requiring tenant context.
     * Intended for /menu/v1/brands on the public homepage.
     */
    public List<BrandDto> getAllBrandsPublic() {
        return brandRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BrandDto> getMyBrands() {
        // Возвращаем только бренды, в которых у пользователя есть членство (ADMIN или OWNER),
        // и только внутри текущего master-контекста (если он задан).
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("Not authenticated");
        }
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Long masterId = TenantContext.getMasterId();
        if (masterId == null) {
            // Без контекста бренды не возвращаем (новый пользователь увидит пусто, но сможет создать бренд)
            return List.of();
        }

        List<UserMembership> memberships = userMembershipRepository.findByUserId(currentUser.getId());
        List<Long> brandIds = memberships.stream()
                .filter(m -> m.getBrand() != null)
                .filter(m -> m.getMaster() != null && masterId.equals(m.getMaster().getId()))
                .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()))
                .filter(m -> m.getRole() == RoleMembership.ADMIN || m.getRole() == RoleMembership.OWNER)
                .map(m -> m.getBrand().getId())
                .distinct()
                .collect(Collectors.toList());

        if (brandIds.isEmpty()) {
            return List.of();
        }

        return brandRepository.findByIdIn(brandIds)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BrandDto createBrand(BrandDto brandDto) {
        // 1) Определяем masterId: из TenantContext, либо по текущему пользователю (создаём MasterAccount при необходимости)
        Long masterId = TenantContext.getMasterId();
        User currentUser = null;
        if (masterId == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                throw new ResourceNotFoundException("Not authenticated");
            }
            String username = auth.getName();
            currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            // Найти/создать MasterAccount для пользователя (имя = username)
            MasterAccount master = masterAccountRepository.findByName(username)
                    .orElseGet(() -> masterAccountRepository.save(MasterAccount.builder()
                            .name(username)
                            .status("ACTIVE")
                            .build()));
            masterId = master.getId();
        }

        // 2) Квота: не более 2 брендов на master
        List<Brand> existing = brandRepository.findByMaster_Id(masterId);
        if (existing.size() >= 2) {
            throw new ResourceAlreadyExistsException("Brand creation limit reached for current master (max 2)");
        }

        // 3) Проверка уникальности имени в рамках master
        if (brandRepository.existsByNameAndMaster_Id(brandDto.getName(), masterId)) {
            throw new ResourceAlreadyExistsException("Brand already exists with name in this master: " + brandDto.getName());
        }

        // 4) Создание бренда
        Brand brand = Brand.builder()
                .name(brandDto.getName())
                .organizationName(brandDto.getOrganizationName())
                .build();
        MasterAccount masterRef = new MasterAccount();
        masterRef.setId(masterId);
        brand.setMaster(masterRef);
        Brand savedBrand = brandRepository.save(brand);

        // 5) Создать дефолтную активную точку самовывоза
        PickupPoint pp = PickupPoint.builder()
                .brand(savedBrand)
                .name("Основная точка")
                .address("Адрес не указан")
                .active(true)
                .build();
        pp = pickupPointRepository.save(pp);

        // 6) Создать membership владельца бренда (OWNER) для текущего пользователя
        if (currentUser == null) {
            // если masterId пришёл из контекста, всё равно найдём текущего пользователя
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                String username = auth.getName();
                currentUser = userRepository.findByUsername(username)
                        .orElse(null);
            }
        }
        if (currentUser != null) {
            UserMembership um = UserMembership.builder()
                    .user(currentUser)
                    .master(masterRef)
                    .brand(savedBrand)
                    .pickupPoint(pp)
                    .role(RoleMembership.OWNER)
                    .status("ACTIVE")
                    .build();
            userMembershipRepository.save(um);

            // Ассоциировать бренд с пользователем (для удобства выборок)
            currentUser.getBrands().add(savedBrand);
            userRepository.save(currentUser);

            // Также создаём user-brand membership (лояльность/доступ), чтобы владелец видел заказы бренда и получал нотификации
            try {
                var ubm = kirillzhdanov.identityservice.model.userbrand.UserBrandMembership.builder()
                        .user(currentUser)
                        .brand(savedBrand)
                        .build();
                userBrandMembershipRepository.save(ubm);
            } catch (Exception ignored) {
            }
        }

        return convertToDto(savedBrand);
    }

    @Transactional
    public BrandDto updateBrand(Long id, BrandDto brandDto) {
        Long masterId = TenantContext.getMasterIdOrThrow();
        Brand brand = brandRepository.findByIdAndMaster_Id(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id in current master: " + id));

        brand.setName(brandDto.getName());
        Brand updatedBrand = brandRepository.save(brand);
        return convertToDto(updatedBrand);
    }

    @Transactional
    public void deleteBrand(Long id) {
        Long masterId = TenantContext.getMasterIdOrThrow();
        Brand brand = brandRepository.findByIdAndMaster_Id(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id in current master: " + id));
        brandRepository.delete(brand);
    }

    @Transactional
    public void assignUserToBrand(Long userId, Long brandId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + brandId));

        user.getBrands()
                .add(brand);
        userRepository.save(user);
    }

    @Transactional
    public void removeUserFromBrand(Long userId, Long brandId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + brandId));

        user.getBrands()
                .remove(brand);
        userRepository.save(user);
    }

    private BrandDto convertToDto(Brand brand) {
        return BrandDto.builder()
                .id(brand.getId())
                .name(brand.getName())
                .organizationName(brand.getOrganizationName())
                .build();
    }

    public BrandDto getBrandById(Long id) {
        Long masterId = TenantContext.getMasterIdOrThrow();
        Brand brand = brandRepository.findByIdAndMaster_Id(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id in current master: " + id));
        return convertToDto(brand);
    }

    /**
     * Public fetch by ID without requiring tenant context; used by public menu endpoints.
     */
    public BrandDto getBrandByIdPublic(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        return convertToDto(brand);
    }
}
