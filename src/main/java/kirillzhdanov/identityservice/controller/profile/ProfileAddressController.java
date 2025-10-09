package kirillzhdanov.identityservice.controller.profile;

import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.AddressDto;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.userbrand.DeliveryAddress;
import kirillzhdanov.identityservice.model.userbrand.UserBrandMembership;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.userbrand.DeliveryAddressRepository;
import kirillzhdanov.identityservice.repository.userbrand.UserBrandMembershipRepository;
import kirillzhdanov.identityservice.tenant.ContextAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Контроллер адресов доставки в личном кабинете пользователя.
 * <p>
 * Адреса хранятся в привязке к членству пользователя в бренде (UserBrandMembership),
 * чтобы персонал конкретного бренда мог корректно обрабатывать заказы.
 */
@RestController
@RequestMapping("/profile/v1/addresses")
@RequiredArgsConstructor
public class ProfileAddressController {

    private final DeliveryAddressRepository deliveryAddressRepository;
    private final UserBrandMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    /**
     * Возвращает список адресов доставки текущего пользователя для выбранного бренда (по контексту).
     * Если контекст бренда не задан — возвращает пустой список.
     */
    @GetMapping
    public ResponseEntity<List<AddressDto>> list() {
        Optional<UserBrandMembership> mb = resolveCurrentMembership();
        if (mb.isEmpty()) return ResponseEntity.ok(List.of());
        List<DeliveryAddress> list = deliveryAddressRepository.findByMembership_IdAndDeletedFalse(mb.get().getId());
        return ResponseEntity.ok(list.stream().map(this::toDto).collect(Collectors.toList()));
    }

    /**
     * Создаёт новый адрес доставки для текущего пользователя в контексте выбранного бренда.
     */
    @PostMapping
    public ResponseEntity<AddressDto> create(@Valid @RequestBody AddressDto dto) {
        Optional<UserBrandMembership> mb = resolveCurrentMembership();
        if (mb.isEmpty()) return ResponseEntity.status(403).build();
        DeliveryAddress a = new DeliveryAddress();
        a.setMembership(mb.get());
        a.setLine1(dto.getLine1());
        a.setLine2(dto.getLine2());
        a.setCity(dto.getCity());
        a.setRegion(dto.getRegion());
        a.setPostcode(dto.getPostcode());
        a.setComment(dto.getComment());
        a.setDeleted(Boolean.FALSE);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        a = deliveryAddressRepository.save(a);
        return ResponseEntity.ok(toDto(a));
    }

    /**
     * Помечает адрес как удалённый (soft delete). Доступно только владельцу адреса в рамках текущего контекста.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable Long id) {
        Optional<UserBrandMembership> mb = resolveCurrentMembership();
        if (mb.isEmpty()) return ResponseEntity.status(403).build();
        Optional<DeliveryAddress> opt = deliveryAddressRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.noContent().build();
        DeliveryAddress a = opt.get();
        if (!a.getMembership().getId().equals(mb.get().getId())) {
            return ResponseEntity.status(403).build();
        }
        a.setDeleted(Boolean.TRUE);
        a.setUpdatedAt(LocalDateTime.now());
        deliveryAddressRepository.save(a);
        return ResponseEntity.noContent().build();
    }

    private AddressDto toDto(DeliveryAddress a) {
        return AddressDto.builder()
                .id(a.getId())
                .line1(a.getLine1())
                .line2(a.getLine2())
                .city(a.getCity())
                .region(a.getRegion())
                .postcode(a.getPostcode())
                .comment(a.getComment())
                .build();
    }

    /**
     * Находит членство пользователя (user↔brand) из текущего контекста, чтобы связать адрес с брендом.
     */
    private Optional<UserBrandMembership> resolveCurrentMembership() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken)
            return Optional.empty();
        String username = auth.getName();
        Optional<User> u = userRepository.findByUsername(username);
        if (u.isEmpty()) return Optional.empty();
        Long brandId = ContextAccess.getBrandIdOrNull();
        if (brandId == null) return Optional.empty();
        return membershipRepository.findByUser_IdAndBrand_Id(u.get().getId(), brandId);
    }
}
