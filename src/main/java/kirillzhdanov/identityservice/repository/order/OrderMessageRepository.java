package kirillzhdanov.identityservice.repository.order;

import kirillzhdanov.identityservice.model.order.OrderMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderMessageRepository extends JpaRepository<OrderMessage, Long> {
    List<OrderMessage> findByOrder_IdOrderByIdAsc(Long orderId);
}
