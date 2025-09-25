package kirillzhdanov.identityservice.repository.order;

import kirillzhdanov.identityservice.model.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    Page<Order> findAllByBrand_Id(Long brandId, Pageable pageable);

    java.util.List<Order> findByClient_IdOrderByIdDesc(Long clientId);
}
