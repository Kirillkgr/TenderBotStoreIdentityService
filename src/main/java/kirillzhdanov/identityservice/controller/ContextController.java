package kirillzhdanov.identityservice.controller;

import kirillzhdanov.identityservice.dto.ContextSwitchRequest;
import kirillzhdanov.identityservice.dto.ContextSwitchResponse;
import kirillzhdanov.identityservice.exception.BadRequestException;
import kirillzhdanov.identityservice.model.Token;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.UserMembership;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import kirillzhdanov.identityservice.security.CustomUserDetails;
import kirillzhdanov.identityservice.security.JwtUtils;
import kirillzhdanov.identityservice.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/auth/v1/context")
@RequiredArgsConstructor
public class ContextController {

    private final UserMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final TokenService tokenService;

    @PostMapping("/switch")
    public ResponseEntity<ContextSwitchResponse> switchContext(@RequestBody ContextSwitchRequest request) {
        if (request.getMembershipId() == null) {
            throw new BadRequestException("membershipId is required");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BadRequestException("Not authenticated");
        }
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        UserMembership membership = membershipRepository.findById(request.getMembershipId())
                .orElseThrow(() -> new BadRequestException("Membership not found"));
        if (!Objects.equals(membership.getUser().getId(), user.getId())) {
            throw new BadRequestException("Membership does not belong to current user");
        }

        Long masterId = membership.getMaster() != null ? membership.getMaster().getId() : null;
        // validate overrides: if provided and mismatch - 400
        if (request.getBrandId() != null && membership.getBrand() != null && !Objects.equals(request.getBrandId(), membership.getBrand().getId())) {
            throw new BadRequestException("brandId override does not match membership");
        }
        if (request.getLocationId() != null && membership.getPickupPoint() != null && !Objects.equals(request.getLocationId(), membership.getPickupPoint().getId())) {
            throw new BadRequestException("locationId override does not match membership");
        }
        Long brandId = request.getBrandId() != null ? request.getBrandId() : (membership.getBrand() != null ? membership.getBrand().getId() : null);
        Long locationId = request.getLocationId() != null ? request.getLocationId() : (membership.getPickupPoint() != null ? membership.getPickupPoint().getId() : null);

        String accessToken = jwtUtils.generateAccessToken(new CustomUserDetails(user),
                membership.getId(), masterId, brandId, locationId);
        tokenService.saveToken(accessToken, Token.TokenType.ACCESS, user);
        return ResponseEntity.ok(new ContextSwitchResponse(accessToken));
    }
}
