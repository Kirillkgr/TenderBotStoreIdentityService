package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MasterAccountService {

    private final MasterAccountRepository masterAccountRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long resolveOrCreateMasterIdForCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("Not authenticated");
        }
        String username = auth.getName();
        // ensure user exists
        userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        MasterAccount master = masterAccountRepository.findByName(username)
                .orElseGet(() -> masterAccountRepository.save(MasterAccount.builder()
                        .name(username)
                        .status("ACTIVE")
                        .build()));
        return master.getId();
    }
}
