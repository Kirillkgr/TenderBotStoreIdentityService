package kirillzhdanov.identityservice.service.impl;

import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.userbrand.UserBrandMembership;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.userbrand.UserBrandMembershipRepository;
import kirillzhdanov.identityservice.service.MembershipService;
import kirillzhdanov.identityservice.util.BrandContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private final UserRepository userRepository;
    private final BrandRepository brandRepository;
    private final UserBrandMembershipRepository membershipRepository;

    @Override
    public void ensureMembershipForUsernameInCurrentBrand(String username) {
        if (username == null || username.isBlank()) return;
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();

        Long brandId = BrandContextHolder.get();
        if (brandId == null) {
            var first = brandRepository.findAll(PageRequest.of(0, 1, Sort.by("id").ascending()));
            if (!first.isEmpty()) brandId = first.getContent().get(0).getId();
        }
        if (brandId == null) {
            log.warn("No brand context found to create membership for user={}", username);
            return;
        }

        Brand brand = brandRepository.findById(brandId).orElse(null);
        if (brand == null) {
            log.warn("Brand {} not found to create membership for user={}", brandId, username);
            return;
        }

        boolean exists = membershipRepository.findByUser_IdAndBrand_Id(user.getId(), brand.getId()).isPresent();
        if (exists) return;

        UserBrandMembership mb = UserBrandMembership.builder()
                .user(user)
                .brand(brand)
                .build();
        membershipRepository.save(mb);
    }
}
