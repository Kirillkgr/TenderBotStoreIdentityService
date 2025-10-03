package kirillzhdanov.identityservice.repository;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
public class BrandRepositoryIsolationTest extends IntegrationTestBase {

    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private MasterAccountRepository masterAccountRepository;

    @Test
    void find_by_master_only_returns_that_master() {
        MasterAccount m1 = new MasterAccount();
        m1.setName("M1");
        MasterAccount m2 = new MasterAccount();
        m2.setName("M2");
        m1 = masterAccountRepository.save(m1);
        m2 = masterAccountRepository.save(m2);
        // Persist masters via brands (no direct repo here)
        Brand b1 = Brand.builder().name("A").build();
        b1.setMaster(m1);
        Brand b2 = Brand.builder().name("B").build();
        b2.setMaster(m2);
        brandRepository.saveAll(List.of(b1, b2));

        // m1 only
        List<Brand> onlyM1 = brandRepository.findByMaster_Id(b1.getMaster().getId());
        assertTrue(onlyM1.stream().allMatch(x -> x.getMaster().getId().equals(b1.getMaster().getId())));

        // m2 only
        List<Brand> onlyM2 = brandRepository.findByMaster_Id(b2.getMaster().getId());
        assertTrue(onlyM2.stream().allMatch(x -> x.getMaster().getId().equals(b2.getMaster().getId())));
    }

    @Test
    void find_by_id_and_master_blocks_cross_access() {
        MasterAccount m1 = new MasterAccount();
        m1.setName("M1");
        MasterAccount m2 = new MasterAccount();
        m2.setName("M2");
        m1 = masterAccountRepository.save(m1);
        m2 = masterAccountRepository.save(m2);
        Brand b1 = Brand.builder().name("A").build();
        b1.setMaster(m1);
        Brand b2 = Brand.builder().name("B").build();
        b2.setMaster(m2);
        brandRepository.saveAll(List.of(b1, b2));

        assertTrue(brandRepository.findByIdAndMaster_Id(b1.getId(), b1.getMaster().getId()).isPresent());
        assertTrue(brandRepository.findByIdAndMaster_Id(b2.getId(), b2.getMaster().getId()).isPresent());

        // cross access should be empty
        assertTrue(brandRepository.findByIdAndMaster_Id(b1.getId(), b2.getMaster().getId()).isEmpty());
        assertTrue(brandRepository.findByIdAndMaster_Id(b2.getId(), b1.getMaster().getId()).isEmpty());
    }
}
