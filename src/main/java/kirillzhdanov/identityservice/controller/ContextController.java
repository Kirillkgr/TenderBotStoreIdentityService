package kirillzhdanov.identityservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kirillzhdanov.identityservice.dto.ContextSwitchRequest;
import kirillzhdanov.identityservice.dto.ContextSwitchResponse;
import kirillzhdanov.identityservice.dto.CtxSetRequest;
import kirillzhdanov.identityservice.exception.BadRequestException;
import kirillzhdanov.identityservice.model.Token;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.UserMembership;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import kirillzhdanov.identityservice.security.CustomUserDetails;
import kirillzhdanov.identityservice.security.JwtUtils;
import kirillzhdanov.identityservice.service.TokenService;
import kirillzhdanov.identityservice.util.HmacSigner;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

/**
 * Контроллер для управления "рабочим контекстом" пользователя.
 * Простой аналог: выбор бренда и точки самовывоза сохраняется в HttpOnly cookie,
 * чтобы все запросы понимали, в каком контексте действует пользователь.
 */
@RestController
@RequestMapping("/auth/v1/context")
@RequiredArgsConstructor
public class ContextController {

    private final UserMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final TokenService tokenService;

    @Value("${ctx.cookie.secret:change-me}")
    private String ctxSecret;
    @Value("${ctx.cookie.maxAgeSeconds:604800}") // 7 days by default
    private int ctxMaxAge;

    /**
     * Переключает контекст на указанный membership (связку пользователь↔бренд↔точка).
     * Выдаёт новый accessToken, в котором зашиты данные контекста, — удобно для мобильных клиентов.
     * <p>
     * Для веб-клиента основной механизм — HttpOnly cookie (см. {@link #setContext(CtxSetRequest, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}).
     */
    @PostMapping("/switch")
    @Operation(summary = "Переключение контекста (membership)", description = "Требуется аутентификация. Меняет активный контекст и выдаёт новый accessToken. Ошибки: 400 при несоответствии membership/brand/location, 401 без аутентификации.")
    public ResponseEntity<ContextSwitchResponse> switchContext(@RequestBody ContextSwitchRequest request,
                                                              HttpServletRequest httpReq,
                                                              HttpServletResponse httpResp) {
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

        // Дополнительно: устанавливаем HttpOnly cookie "ctx", чтобы ContextEnforcementFilter видел контекст без JWT-клейм
        try {
            long issuedAt = System.currentTimeMillis();
            String json = "{" +
                    (masterId != null ? "\"masterId\":" + masterId + "," : "") +
                    (brandId != null ? "\"brandId\":" + brandId + "," : "") +
                    (locationId != null ? "\"pickupPointId\":" + locationId + "," : "") +
                    "\"issuedAt\":" + issuedAt +
                    "}";
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            HmacSigner signer = new HmacSigner(ctxSecret);
            String sig = signer.signBase64Url(payload);
            String value = payload + "." + sig;
            boolean secure = httpReq.isSecure();
            ResponseCookie cookie = ResponseCookie.from("ctx", value)
                    .httpOnly(true)
                    .secure(secure)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(ctxMaxAge)
                    .build();
            httpResp.addHeader("Set-Cookie", cookie.toString());
        } catch (Exception ignored) {}

        return ResponseEntity.ok(new ContextSwitchResponse(accessToken));
    }

    /**
     * Устанавливает контекст в HttpOnly cookie "ctx".
     * Простой сценарий: фронт отправляет выбранные brandId/pickupPointId, сервер проверяет права и
     * записывает подписанное cookie. Далее все запросы автоматически работают в этом контексте.
     */
    @PostMapping
    @Operation(summary = "Установить контекст через httpOnly cookie ctx", description = "Проверяет права пользователя на указанный master/brand/pickup и устанавливает подписанное cookie ctx")
    public ResponseEntity<Void> setContext(@RequestBody CtxSetRequest req,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BadRequestException("Not authenticated");
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new BadRequestException("User not found"));

        Long masterId = req.getMasterId();
        Long brandId = req.getBrandId();
        Long pickupId = req.getPickupPointId();

        // Derive/validate membership
        Optional<UserMembership> mOpt = Optional.empty();
        if (brandId != null) {
            mOpt = membershipRepository.findByUserIdAndBrandId(user.getId(), brandId);
        } else if (masterId != null) {
            mOpt = membershipRepository.findByUserIdAndMasterId(user.getId(), masterId);
        } else {
            // fallback: if user has single membership, allow using it
            var list = membershipRepository.findByUserId(user.getId());
            if (list.size() == 1) mOpt = Optional.of(list.get(0));
        }
        UserMembership mem = mOpt.orElseThrow(() -> new BadRequestException("No membership for provided context"));

        // Normalize values from membership if not explicitly provided
        if (masterId == null && mem.getMaster() != null) masterId = mem.getMaster().getId();
        if (brandId == null && mem.getBrand() != null) brandId = mem.getBrand().getId();
        if (pickupId == null && mem.getPickupPoint() != null) pickupId = mem.getPickupPoint().getId();

        // Validate provided overrides against membership
        if (req.getBrandId() != null && (mem.getBrand() == null || !Objects.equals(brandId, mem.getBrand().getId()))) {
            throw new BadRequestException("brandId does not belong to user");
        }
        if (req.getPickupPointId() != null && (mem.getPickupPoint() == null || !Objects.equals(pickupId, mem.getPickupPoint().getId()))) {
            throw new BadRequestException("pickupPointId does not belong to user");
        }

        // Build signed cookie value
        long issuedAt = System.currentTimeMillis();
        String json = "{" +
                (masterId != null ? "\"masterId\":" + masterId + "," : "") +
                (brandId != null ? "\"brandId\":" + brandId + "," : "") +
                (pickupId != null ? "\"pickupPointId\":" + pickupId + "," : "") +
                "\"issuedAt\":" + issuedAt +
                "}";
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        HmacSigner signer = new HmacSigner(ctxSecret);
        String sig = signer.signBase64Url(payload);
        String value = payload + "." + sig;

        boolean secure = request.isSecure();
        ResponseCookie cookie = ResponseCookie.from("ctx", value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(ctxMaxAge)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.noContent().build();
    }

    /**
     * Очищает контекст: стирает HttpOnly cookie "ctx". Полезно при смене аккаунта или бренда.
     */
    @DeleteMapping
    @Operation(summary = "Очистить контекст (ctx cookie)")
    public ResponseEntity<Void> clearContext(HttpServletRequest request, HttpServletResponse response) {
        boolean secure = request.isSecure();
        ResponseCookie cookie = ResponseCookie.from("ctx", "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.noContent().build();
    }
}
