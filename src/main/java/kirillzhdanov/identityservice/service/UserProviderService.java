package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.UserProvider;
import kirillzhdanov.identityservice.repository.UserProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProviderService {

    private final UserProviderRepository userProviderRepository;

    public UserProviderService(UserProviderRepository userProviderRepository) {
        this.userProviderRepository = userProviderRepository;
    }

    @Transactional
    public void unlink(User user, UserProvider.Provider provider) {
        if (user == null || provider == null) return;
        userProviderRepository.deleteByUserAndProvider(user, provider);
    }
}
