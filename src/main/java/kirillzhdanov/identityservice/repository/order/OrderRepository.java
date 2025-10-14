package kirillzhdanov.identityservice.repository.order;

import kirillzhdanov.identityservice.model.order.Order;
import kirillzhdanov.identityservice.dto.client.ClientProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    Page<Order> findAllByBrand_Id(Long brandId, Pageable pageable);

    java.util.List<Order> findByClient_IdOrderByIdDesc(Long clientId);

    Order findTopByClient_IdAndMaster_IdOrderByCreatedAtDesc(Long clientId, Long masterId);

    @Query(value = """
            with lo as (
                select distinct on (o.client_id)
                       o.client_id,
                       o.created_at    as last_order_at,
                       o.brand_id      as last_order_brand_id
                  from orders o
                 where o.master_id = :masterId
                 order by o.client_id, o.created_at desc
            )
            select 
              u.id            as id,
              u.first_name    as firstName,
              u.last_name     as lastName,
              u.patronymic    as patronymic,
              u.email         as email,
              u.phone         as phone,
              u.date_of_birth as dateOfBirth,
              lo.last_order_at       as lastOrderAt,
              lo.last_order_brand_id as lastOrderBrandId,
              b.name                 as lastOrderBrand
            from lo
            join users u on u.id = lo.client_id
            left join brands b on b.id = lo.last_order_brand_id
            where (
              :search is null 
              or u.username   ilike concat('%', :search, '%')
              or u.first_name ilike concat('%', :search, '%')
              or u.last_name  ilike concat('%', :search, '%')
              or u.patronymic ilike concat('%', :search, '%')
              or u.email      ilike concat('%', :search, '%')
              or u.phone      ilike concat('%', :search, '%')
            )
            """,
            countQuery = """
                    with lo as (
                        select distinct on (o.client_id)
                               o.client_id,
                               o.created_at    as last_order_at,
                               o.brand_id      as last_order_brand_id
                          from orders o
                         where o.master_id = :masterId
                         order by o.client_id, o.created_at desc
                    )
                    select count(1)
                    from lo
                    join users u on u.id = lo.client_id
                    where (
                      :search is null 
                      or u.username   ilike concat('%', :search, '%')
                      or u.first_name ilike concat('%', :search, '%')
                      or u.last_name  ilike concat('%', :search, '%')
                      or u.patronymic ilike concat('%', :search, '%')
                      or u.email      ilike concat('%', :search, '%')
                      or u.phone      ilike concat('%', :search, '%')
                    )
                    """,
            nativeQuery = true)
    Page<ClientProjection> findClientsByMasterWithLastOrder(Long masterId, String search, Pageable pageable);
}
