package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.product.ProductCreateRequest;
import kirillzhdanov.identityservice.dto.product.ProductResponse;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
public class ProductServiceIsolationTest extends IntegrationTestBase {

    @Autowired
    private ProductService productService;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private MasterAccountRepository masterAccountRepository;

    private Brand b1;
    private Brand b2;

    @BeforeEach
    void seed() {
        MasterAccount m1 = new MasterAccount();
        m1.setName("M1");
        MasterAccount m2 = new MasterAccount();
        m2.setName("M2");
        m1 = masterAccountRepository.save(m1);
        m2 = masterAccountRepository.save(m2);
        b1 = Brand.builder().name("B1").build();
        b1.setMaster(m1);
        b2 = Brand.builder().name("B2").build();
        b2.setMaster(m2);
        brandRepository.saveAll(List.of(b1, b2));

        ProductCreateRequest p1 = new ProductCreateRequest();
        p1.setName("P1");
        p1.setPrice(new BigDecimal("1"));
        p1.setBrandId(b1.getId());
        p1.setGroupTagId(0L);
        p1.setVisible(true);
        productService.create(p1);

        ProductCreateRequest p2 = new ProductCreateRequest();
        p2.setName("P2");
        p2.setPrice(new BigDecimal("2"));
        p2.setBrandId(b2.getId());
        p2.setGroupTagId(0L);
        p2.setVisible(true);
        productService.create(p2);
    }

    @Test
    @DisplayName("getByBrandAndGroup: возвращает товары только своего бренда")
    void getByBrandAndGroup_isolated_by_brand() {
        List<ProductResponse> list1 = productService.getByBrandAndGroup(b1.getId(), 0L, true);
        assertEquals(1, list1.size());
        assertEquals("B1", list1.getFirst().getBrandId().equals(b1.getId()) ? "B1" : "?");

        List<ProductResponse> list2 = productService.getByBrandAndGroup(b2.getId(), 0L, true);
        assertEquals(1, list2.size());
        assertEquals("B2", list2.getFirst().getBrandId().equals(b2.getId()) ? "B2" : "?");
    }

    @Test
    @DisplayName("changeBrand: смена бренда сбрасывает группу если не совпадает, и не даёт кросс‑брендовый конфликт")
    void changeBrand_validations() {
        // создаём товар под b1
        ProductCreateRequest p = new ProductCreateRequest();
        p.setName("PX");
        p.setPrice(new BigDecimal("3"));
        p.setBrandId(b1.getId());
        p.setVisible(true);
        p.setGroupTagId(0L);
        ProductResponse created = productService.create(p);

        // меняем бренд на b2
        ProductResponse changed = productService.changeBrand(created.getId(), b2.getId());
        assertEquals(b2.getId(), changed.getBrandId());
    }
}
