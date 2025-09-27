package kirillzhdanov.identityservice.repository.order;

import kirillzhdanov.identityservice.model.order.OrderReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderReviewRepository extends JpaRepository<OrderReview, Long> {
    Optional<OrderReview> findByOrder_Id(Long orderId);
}
