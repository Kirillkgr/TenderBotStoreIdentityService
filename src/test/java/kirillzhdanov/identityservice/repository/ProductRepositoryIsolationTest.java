package kirillzhdanov.identityservice.repository;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.model.product.Product;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProductRepositoryIsolationTest extends IntegrationTestBase {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private MasterAccountRepository masterAccountRepository;

    @Test
    void products_filtered_by_brand_and_master() {
        MasterAccount m1 = new MasterAccount();
        m1.setName("M1");
        MasterAccount m2 = new MasterAccount();
        m2.setName("M2");
        m1 = masterAccountRepository.save(m1);
        m2 = masterAccountRepository.save(m2);
        Brand b1 = Brand.builder().name("B1").build();
        b1.setMaster(m1);
        Brand b2 = Brand.builder().name("B2").build();
        b2.setMaster(m2);
        brandRepository.saveAll(List.of(b1, b2));

        Product p1 = new Product();
        p1.setName("P1");
        p1.setPrice(new BigDecimal("1"));
        p1.setBrand(b1);
        Product p2 = new Product();
        p2.setName("P2");
        p2.setPrice(new BigDecimal("2"));
        p2.setBrand(b2);
        productRepository.saveAll(List.of(p1, p2));

        // По бренду b1
        var onlyB1 = productRepository.findByBrandAndGroupTagIsNull(b1);
        assertEquals(1, onlyB1.size());
        assertEquals("P1", onlyB1.getFirst().getName());

        // По бренду b2
        var onlyB2 = productRepository.findByBrandAndGroupTagIsNull(b2);
        assertEquals(1, onlyB2.size());
        assertEquals("P2", onlyB2.getFirst().getName());
    }
}
