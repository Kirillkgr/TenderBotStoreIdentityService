package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.dto.BrandDto;
import kirillzhdanov.identityservice.exception.ResourceAlreadyExistsException;
import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

	public List<BrandDto> getAllBrands() {

		return brandRepository.findAll()
				.stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}

	/**
	 * Returns brands assigned to the currently authenticated user only.
	 */
	@Transactional(readOnly = true)
	public List<BrandDto> getMyBrands() {

		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		User currentUser = userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));

		return currentUser.getBrands()
				.stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}

	@Transactional
	public BrandDto createBrand(BrandDto brandDto) {

		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		User currentUser = userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));

		if (brandRepository.existsByName(brandDto.getName())) {
			throw new ResourceAlreadyExistsException("Brand already exists with name: " + brandDto.getName());
		}

		Brand brand = Brand.builder()
						   .name(brandDto.getName())
						   .organizationName(brandDto.getOrganizationName())
						   .build();

		Brand savedBrand = brandRepository.save(brand);

		currentUser.getBrands().add(savedBrand);
		userRepository.save(currentUser);

		return convertToDto(savedBrand);
	}

	@Transactional
	public BrandDto updateBrand(Long id, BrandDto brandDto) {

		Brand brand = brandRepository.findById(id)
									 .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));

		brand.setName(brandDto.getName());
		Brand updatedBrand = brandRepository.save(brand);
		return convertToDto(updatedBrand);
	}

	@Transactional
	public void deleteBrand(Long id) {

		if (!brandRepository.existsById(id)) {
			throw new ResourceNotFoundException("Brand not found with id: " + id);
		}
		brandRepository.deleteById(id);
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

		Brand brand = brandRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
		return convertToDto(brand);
	}
}
