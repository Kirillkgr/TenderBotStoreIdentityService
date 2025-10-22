package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.dto.JwtUserDetailsResponse;
import kirillzhdanov.identityservice.model.User;

import java.util.Optional;

public interface UserService {
    JwtUserDetailsResponse getUserDetailsById(Long userId);

    boolean existsByUsername(String username);

    User saveNewUser(User user);

    User save(User user);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);
}
