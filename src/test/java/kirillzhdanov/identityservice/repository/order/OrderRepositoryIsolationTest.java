package kirillzhdanov.identityservice.repository.order;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.model.order.DeliveryMode;
import kirillzhdanov.identityservice.model.order.Order;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrderRepositoryIsolationTest extends IntegrationTestBase {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MasterAccountRepository masterAccountRepository;

    @Test
    void orders_filtered_by_brand_and_client() {
        // masters/brands
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

        // users
        User u1 = User.builder().username("u1").password("p").build();
        User u2 = User.builder().username("u2").password("p").build();
        userRepository.saveAll(List.of(u1, u2));

        // orders
        Order o1 = Order.builder().client(u1).brand(b1).total(new BigDecimal("10")).deliveryMode(DeliveryMode.PICKUP).build();
        Order o2 = Order.builder().client(u2).brand(b2).total(new BigDecimal("20")).deliveryMode(DeliveryMode.PICKUP).build();
        orderRepository.saveAll(List.of(o1, o2));

        // by brand (b1)
        Page<Order> pageB1 = orderRepository.findAllByBrand_Id(b1.getId(), PageRequest.of(0, 10));
        assertEquals(1, pageB1.getTotalElements());
        assertEquals(o1.getId(), pageB1.getContent().getFirst().getId());

        // by brand (b2)
        Page<Order> pageB2 = orderRepository.findAllByBrand_Id(b2.getId(), PageRequest.of(0, 10));
        assertEquals(1, pageB2.getTotalElements());
        assertEquals(o2.getId(), pageB2.getContent().getFirst().getId());

        // by client
        var byU1 = orderRepository.findByClient_IdOrderByIdDesc(u1.getId());
        assertEquals(1, byU1.size());
        assertEquals(o1.getId(), byU1.getFirst().getId());

        var byU2 = orderRepository.findByClient_IdOrderByIdDesc(u2.getId());
        assertEquals(1, byU2.size());
        assertEquals(o2.getId(), byU2.getFirst().getId());
    }
}
