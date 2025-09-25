package kirillzhdanov.identityservice.service.admin;

import kirillzhdanov.identityservice.dto.order.OrderDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderAdminService {
    Page<OrderDto> findOrders(Pageable pageable, String search, Long brandId, String dateFrom, String dateTo);

    Page<OrderDto> findOrdersForBrands(Pageable pageable, String search, List<Long> brandIds, String dateFrom, String dateTo);
}
