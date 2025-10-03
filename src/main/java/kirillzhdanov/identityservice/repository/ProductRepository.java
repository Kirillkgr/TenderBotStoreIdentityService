package kirillzhdanov.identityservice.repository;

import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Корневые товары (groupTag IS NULL)
    List<Product> findByBrandAndGroupTagIsNull(Brand brand);
    List<Product> findByBrandAndGroupTagIsNullAndVisibleIsTrue(Brand brand);

    // Товары в конкретной группе
    List<Product> findByBrandAndGroupTagId(Brand brand, Long groupTagId);
    List<Product> findByBrandAndGroupTagIdAndVisibleIsTrue(Brand brand, Long groupTagId);

    // Поиск товара по id с учётом мастера бренда
    Optional<Product> findByIdAndBrand_Master_Id(Long id, Long masterId);
}
