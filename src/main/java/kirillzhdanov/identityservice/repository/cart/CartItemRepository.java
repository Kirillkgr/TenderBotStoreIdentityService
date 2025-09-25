package kirillzhdanov.identityservice.repository.cart;

import kirillzhdanov.identityservice.model.cart.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @EntityGraph(attributePaths = {"product", "brand"})
    List<CartItem> findByUser_Id(Long userId);

    @EntityGraph(attributePaths = {"product", "brand"})
    List<CartItem> findByCartToken(String cartToken);

    void deleteByUser_Id(Long userId);

    void deleteByCartToken(String cartToken);

    @EntityGraph(attributePaths = {"product", "brand"})
    List<CartItem> findByUser_IsNullAndUpdatedAtBefore(LocalDateTime threshold);
}
