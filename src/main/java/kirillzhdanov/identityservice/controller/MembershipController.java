package kirillzhdanov.identityservice.controller;

import kirillzhdanov.identityservice.dto.MembershipDto;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.UserMembership;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth/v1/memberships")
@RequiredArgsConstructor
public class MembershipController {

    private final UserMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<MembershipDto>> myMemberships() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String username = auth.getName();
        Long userId = userRepository.findByUsername(username).map(User::getId).orElse(null);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<UserMembership> list = membershipRepository.findByUserId(userId);

        List<MembershipDto> result = list.stream().map(um -> MembershipDto.builder()
                .membershipId(um.getId())
                .masterId(um.getMaster() != null ? um.getMaster().getId() : null)
                .masterName(um.getMaster() != null ? um.getMaster().getName() : null)
                .brandId(um.getBrand() != null ? um.getBrand().getId() : null)
                .brandName(um.getBrand() != null ? um.getBrand().getName() : null)
                .locationId(um.getPickupPoint() != null ? um.getPickupPoint().getId() : null)
                .locationName(um.getPickupPoint() != null ? um.getPickupPoint().getName() : null)
                .role(um.getRole())
                .status(um.getStatus())
                .build()).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}
