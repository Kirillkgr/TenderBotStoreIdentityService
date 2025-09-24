package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.order.DeliveryMode;
import kirillzhdanov.identityservice.model.order.Order;

public interface CheckoutService {
    Order createOrderFromCart(User user,
                              DeliveryMode mode,
                              Long addressId,
                              Long pickupPointId,
                              String comment);
}
