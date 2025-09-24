package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.model.User;

public interface CartService {
    void mergeGuestCartToUser(User user, String cartToken);
}
