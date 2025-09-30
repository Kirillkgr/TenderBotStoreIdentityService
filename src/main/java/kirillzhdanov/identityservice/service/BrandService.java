package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.dto.BrandDto;
import kirillzhdanov.identityservice.exception.ResourceAlreadyExistsException;
import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static kirillzhdanov.identityservice.tenant.TenantContext.getMasterIdOrThrow;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final UserRepository userRepository;

    public List<BrandDto> getAllBrands() {
        Long masterId = getMasterIdOrThrow();
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
        Long masterId = getMasterIdOrThrow();
        return brandRepository.findByMaster_Id(masterId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BrandDto createBrand(BrandDto brandDto) {
        Long masterId = getMasterIdOrThrow();
        if (brandRepository.existsByNameAndMaster_Id(brandDto.getName(), masterId)) {
            throw new ResourceAlreadyExistsException("Brand already exists with name in this master: " + brandDto.getName());
        }
        Brand brand = Brand.builder()
                .name(brandDto.getName())
                .organizationName(brandDto.getOrganizationName())
                .build();
        MasterAccount masterRef = new MasterAccount();
        masterRef.setId(masterId);
        brand.setMaster(masterRef);
        Brand savedBrand = brandRepository.save(brand);
        return convertToDto(savedBrand);
    }

    @Transactional
    public BrandDto updateBrand(Long id, BrandDto brandDto) {
        Long masterId = getMasterIdOrThrow();
        Brand brand = brandRepository.findByIdAndMaster_Id(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id in current master: " + id));

        brand.setName(brandDto.getName());
        Brand updatedBrand = brandRepository.save(brand);
        return convertToDto(updatedBrand);
    }

    @Transactional
    public void deleteBrand(Long id) {
        Long masterId = getMasterIdOrThrow();
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
        Long masterId = getMasterIdOrThrow();
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
