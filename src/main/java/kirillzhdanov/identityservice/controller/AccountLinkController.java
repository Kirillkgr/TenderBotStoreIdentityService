package kirillzhdanov.identityservice.controller;

import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.UserProvider;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.security.CustomUserDetails;
import kirillzhdanov.identityservice.service.UserProviderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/v1/providers")
public class AccountLinkController {

    private final UserRepository userRepository;
    private final UserProviderService userProviderService;

    public AccountLinkController(UserRepository userRepository, UserProviderService userProviderService) {
        this.userRepository = userRepository;
        this.userProviderService = userProviderService;
    }

    @DeleteMapping("/google")
    public ResponseEntity<Void> unlinkGoogle() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails cud)) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findById(cud.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        userProviderService.unlink(user, UserProvider.Provider.GOOGLE);
        return ResponseEntity.noContent().build();
    }
}
