package kirillzhdanov.identityservice.controller;

import kirillzhdanov.identityservice.dto.BrandDto;
import kirillzhdanov.identityservice.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

	private final BrandService brandService;

	@GetMapping
	public ResponseEntity<List<BrandDto>> getAllBrands(){

		return ResponseEntity.ok(brandService.getAllBrands());
	}

	@GetMapping("/{id}")
	public ResponseEntity<BrandDto> getBrandById(@PathVariable Long id){

		return ResponseEntity.ok(brandService.getBrandById(id));
	}

	@PostMapping
	public ResponseEntity<BrandDto> createBrand(@RequestBody BrandDto brandDto){

		return new ResponseEntity<>(brandService.createBrand(brandDto), HttpStatus.CREATED);
	}

	@PutMapping("/{id}")
	public ResponseEntity<BrandDto> updateBrand(@PathVariable Long id, @RequestBody BrandDto brandDto){

		return ResponseEntity.ok(brandService.updateBrand(id, brandDto));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteBrand(@PathVariable Long id){

		brandService.deleteBrand(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{brandId}/users/{userId}")
	public ResponseEntity<Void> assignUserToBrand(@PathVariable Long userId, @PathVariable Long brandId){

		brandService.assignUserToBrand(userId, brandId);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{brandId}/users/{userId}")
	public ResponseEntity<Void> removeUserFromBrand(@PathVariable Long userId, @PathVariable Long brandId){

		brandService.removeUserFromBrand(userId, brandId);
		return ResponseEntity.ok().build();
	}
}
