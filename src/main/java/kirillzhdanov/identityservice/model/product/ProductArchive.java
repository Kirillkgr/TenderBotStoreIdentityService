package kirillzhdanov.identityservice.model.product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_archive")
@Getter
@Setter
@NoArgsConstructor
public class ProductArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Идентификатор исходного товара (для возможного восстановления)
    @Column(nullable = false)
    private Long originalProductId;

    // Снэпшот основных полей
    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(precision = 18, scale = 2)
    private BigDecimal promoPrice;

    @Column(nullable = false)
    private Long brandId;

    // null означает, что товар был в корне бренда
    private Long groupTagId;

    // Полный путь группы в момент архивации, например: "/1/4/7/"
    @Column(length = 1000)
    private String groupPath;

    // Флаг видимости на момент архивации (для истории)
    private boolean visible;

    @Column(nullable = false)
    private LocalDateTime archivedAt;
}
