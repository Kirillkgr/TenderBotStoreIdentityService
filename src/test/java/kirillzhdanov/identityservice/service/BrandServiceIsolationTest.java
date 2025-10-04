package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.BrandDto;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static kirillzhdanov.identityservice.tenant.TenantContext.clear;
import static kirillzhdanov.identityservice.tenant.TenantContext.setMasterId;
import static org.junit.jupiter.api.Assertions.*;

public class BrandServiceIsolationTest extends IntegrationTestBase {

    @Autowired
    private BrandService brandService;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private MasterAccountRepository masterAccountRepository;

    private Brand b1;
    private Brand b2;

    @BeforeEach
    void data() {
        clear();
        MasterAccount m1 = new MasterAccount();
        m1.setName("M1");
        MasterAccount m2 = new MasterAccount();
        m2.setName("M2");
        m1 = masterAccountRepository.save(m1);
        m2 = masterAccountRepository.save(m2);
        String uniq = String.valueOf(System.nanoTime());
        b1 = Brand.builder().name("Brand-M1-" + uniq).build();
        b1.setMaster(m1);
        b2 = Brand.builder().name("Brand-M2-" + uniq).build();
        b2.setMaster(m2);
        brandRepository.saveAll(java.util.List.of(b1, b2));
    }

    @Test
    @DisplayName("getAllBrands(): видит только бренды своего master")
    void getAllBrands_isolated_by_master() {
        setMasterId(b1.getMaster().getId());
        List<BrandDto> m1List = brandService.getAllBrands();
        assertTrue(m1List.stream().allMatch(d -> d.getName().startsWith("Brand-M1")));

        setMasterId(b2.getMaster().getId());
        List<BrandDto> m2List = brandService.getAllBrands();
        assertTrue(m2List.stream().allMatch(d -> d.getName().startsWith("Brand-M2")));
    }

    @Test
    @DisplayName("getBrandById(): запрещает сквозной доступ (другой master)")
    void getBrandById_blocks_cross_master() {
        setMasterId(b1.getMaster().getId());
        BrandDto ok = brandService.getBrandById(b1.getId());
        assertEquals(b1.getName(), ok.getName());

        // другой master -> ResourceNotFoundException
        assertThrows(kirillzhdanov.identityservice.exception.ResourceNotFoundException.class,
                () -> brandService.getBrandById(b2.getId()));
    }

    @Test
    @DisplayName("getAllBrandsPublic(): публичный список не зависит от контекста")
    void getAllBrandsPublic_is_public() {
        clear(); // без контекста
        List<BrandDto> all = brandService.getAllBrandsPublic();
        // В окружении тестов может быть предзаполнено демо-данными (3+ бренда).
        // Проверяем, что публичный список доступен и содержит как минимум 2 бренда.
        assertTrue(all.size() >= 2);
    }
}
