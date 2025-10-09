package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.order.DeliveryMode;
import kirillzhdanov.identityservice.model.order.Order;

/**
 * Сервис оформления заказа из корзины.
 */
public interface CheckoutService {
    /**
     * Создаёт заказ из текущей корзины пользователя.
     *
     * @param user          текущий пользователь (обязательно)
     * @param mode          режим доставки: DELIVERY или PICKUP
     * @param addressId     идентификатор адреса доставки (обязателен для DELIVERY)
     * @param pickupPointId идентификатор пункта самовывоза (обязателен для PICKUP)
     * @param comment       комментарий к заказу (необязателен)
     * @param cartToken     guest‑идентификатор корзины (если до этого покупал как гость)
     * @return созданный заказ
     */
    Order createOrderFromCart(User user,
                              DeliveryMode mode,
                              Long addressId,
                              Long pickupPointId,
                              String comment,
                              String cartToken);
}
