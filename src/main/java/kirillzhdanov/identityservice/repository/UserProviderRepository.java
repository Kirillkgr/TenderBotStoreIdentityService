package kirillzhdanov.identityservice.repository;

import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.UserProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProviderRepository extends JpaRepository<UserProvider, Long> {
    Optional<UserProvider> findByProviderAndProviderUserId(UserProvider.Provider provider, String providerUserId);
    boolean existsByProviderAndProviderUserId(UserProvider.Provider provider, String providerUserId);

    Optional<UserProvider> findByUserAndProvider(User user, UserProvider.Provider provider);
    boolean existsByUserAndProvider(User user, UserProvider.Provider provider);
    void deleteByUserAndProvider(User user, UserProvider.Provider provider);
}
