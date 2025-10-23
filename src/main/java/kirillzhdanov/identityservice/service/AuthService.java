package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.dto.*;
import kirillzhdanov.identityservice.exception.BadRequestException;
import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.exception.TokenRefreshException;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.Token;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.StorageFileRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import kirillzhdanov.identityservice.repository.pickup.PickupPointRepository;
import kirillzhdanov.identityservice.security.CustomUserDetails;
import kirillzhdanov.identityservice.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;

    private final BrandRepository brandRepository;

    private final RoleService roleService;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtUtils jwtUtils;

    private final TokenService tokenService;
    private final StorageFileRepository storageFileRepository;
    private final S3StorageService s3StorageService;
    private final MasterAccountRepository masterAccountRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final PickupPointRepository pickupPointRepository;
    private final ProvisioningServiceOps provisioningService;

    private final SecureRandom random = new SecureRandom();


    @Transactional
    public boolean checkUniqUsername(String username) {
        return userService.existsByUsername(username);
    }

    @Transactional
    public UserResponse registerUser(UserRegistrationRequest request) {
        validateRegistrationRequest(request);

        User user = buildUserFromRequest(request);
        assignDefaultRoleAndExtras(user, request);
        assignBrands(user, request);
        applyEmailVerification(user);

        // Шифруем пароль и сохраняем пользователя
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        User savedUser = userService.save(user);

        MasterAccount ensuredMaster = provisioningService.ensureMasterAccountForUser(savedUser);
        provisioningService.ensureOwnerMembership(savedUser, ensuredMaster);
        provisioningService.ensureDefaultBrandAndPickup(savedUser, ensuredMaster);

        // Перезагружаем пользователя, чтобы коллекции (бренды) были актуальны
        User reloaded = userService.findByUsername(savedUser.getUsername())
                .orElse(savedUser);

        CustomUserDetails cud = new CustomUserDetails(reloaded);
        String accessToken = jwtUtils.generateAccessToken(cud);
        String refreshToken = jwtUtils.generateRefreshToken(cud);

        // Сохраняем токены для актуального пользователя
        tokenService.saveToken(accessToken, Token.TokenType.ACCESS, reloaded);
        tokenService.saveToken(refreshToken, Token.TokenType.REFRESH, reloaded);

        return buildUserResponse(reloaded, accessToken, refreshToken);
    }

    private void validateRegistrationRequest(UserRegistrationRequest request) {
        if (request == null) {
            throw new BadRequestException("Запрос регистрации пуст");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BadRequestException("Имя пользователя обязательно");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("Пароль обязателен");
        }
        if (userService.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Пользователь с таким именем уже существует");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank() && userService.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Пользователь с таким email уже существует");
        }
    }

    private User buildUserFromRequest(UserRegistrationRequest request) {
        return User.builder().username(request.getUsername()).password(request.getPassword()).firstName(request.getFirstName()).lastName(request.getLastName()).patronymic(request.getPatronymic()).dateOfBirth(request.getDateOfBirth()).email(request.getEmail()).phone(request.getPhone()).emailVerified(false).brands(new HashSet<>()).roles(new HashSet<>()).build();
    }

    private void assignDefaultRoleAndExtras(User user, UserRegistrationRequest request) {
        Role userRole = roleService.getUserRole();
        user.getRoles().add(userRole);

        if (request.getRoleNames() != null && !request.getRoleNames().isEmpty()) {
            for (Role.RoleName rn : request.getRoleNames()) {
                roleService.findByName(rn).ifPresent(role -> {
                    if (!role.getName().equals(Role.RoleName.USER)) {
                        user.getRoles().add(role);
                    }
                });
            }
        }
    }

    private void assignBrands(User user, UserRegistrationRequest request) {
        if (request.getBrandIds() == null || request.getBrandIds().isEmpty()) return;
        Set<Brand> brands = new HashSet<>();
        for (Long brandId : request.getBrandIds()) {
            Brand brand = brandRepository.findById(brandId).orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + brandId));
            brands.add(brand);
        }
        user.setBrands(brands);
    }

    private void applyEmailVerification(User user) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            user.setEmailVerificationCode(generateEmailCode());
            user.setEmailVerificationExpiresAt(LocalDateTime.now().plusMinutes(15));
        }
    }

    private UserResponse buildUserResponse(User user, String accessToken, String refreshToken) {
        String avatarUrl = buildPresignedAvatarUrl(user);
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .patronymic(user.getPatronymic())
                .dateOfBirth(user.getDateOfBirth())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(avatarUrl)
                .emailVerified(user.isEmailVerified())
                .roles(user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet()))
                .brands(
                        user.getBrands().stream()
                                .filter(Objects::nonNull)
                                .map(brand -> BrandDto.builder().id(brand.getId()).name(brand.getName()).build())
                                .collect(Collectors.toSet())
                )
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {

        String requestRefreshToken = request.getRefreshToken();

        // Проверяем, существует ли токен и действителен ли он
        Optional<Token> tokenOptional = tokenService.findByToken(requestRefreshToken);

        if (tokenOptional.isEmpty() || !tokenOptional.get().isValid()) {
            throw new TokenRefreshException("Токен обновления недействителен или истек");
        }

        Token refreshToken = tokenOptional.get();

        // Проверяем тип токена
        if (refreshToken.getTokenType() != Token.TokenType.REFRESH) {
            throw new TokenRefreshException("Неверный тип токена");
        }

        // Получаем пользователя
        User user = refreshToken.getUser();

        // Создаем CustomUserDetails для включения дополнительной информации в токен
        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        // 1) Отзываем старый refresh-токен (ротация)
        tokenService.revokeToken(requestRefreshToken);

        // 2) Генерируем новые токены
        String newAccessToken = jwtUtils.generateAccessToken(customUserDetails);
        String newRefreshToken = jwtUtils.generateRefreshToken(customUserDetails);

        // 3) Сохраняем оба токена
        tokenService.saveToken(newAccessToken, Token.TokenType.ACCESS, user);
        tokenService.saveToken(newRefreshToken, Token.TokenType.REFRESH, user);

        // 4) Возвращаем новый access и новый refresh (refresh будет установлен в HttpOnly cookie на уровне контроллера)
        return TokenRefreshResponse.builder().accessToken(newAccessToken).refreshToken(newRefreshToken).build();
    }

    @Transactional
    public void revokeToken(String token) {
        tokenService.revokeToken(token);
    }

    @Transactional
    public void revokeAllUserTokens(String username) {

        User user = userService.findByUsername(username).orElseThrow(() -> new BadRequestException("Пользователь не найден"));
        tokenService.revokeAllUserTokens(user);
    }

    @Transactional
    public UserResponse login(LoginRequest request) {
        // Аутентифицируем пользователя
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        // Получаем данные пользователя
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow(() -> new BadRequestException("Пользователь не найден"));

        // Единообразная пост-авторизационная провизия (как при регистрации и OAuth2):
        // 1) гарантируем наличие мастер-аккаунта
        // 2) гарантируем членство владельца
        // 3) гарантируем дефолтный бренд и точку самовывоза
        MasterAccount ensuredMaster = provisioningService.ensureMasterAccountForUser(user);
        provisioningService.ensureOwnerMembership(user, ensuredMaster);
        provisioningService.ensureDefaultBrandAndPickup(user, ensuredMaster);

        // Перечитываем пользователя, чтобы получить актуальные коллекции (brands/memberships)
        user = userService.findByUsername(user.getUsername()).orElse(user);

        // Отзываем все существующие токены пользователя (опционально)
        tokenService.revokeAllUserTokens(user);

        // Создаем CustomUserDetails для включения дополнительной информации в токен
        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        // Генерируем новые токены
        String accessToken = jwtUtils.generateAccessToken(customUserDetails);
        String refreshToken = jwtUtils.generateRefreshToken(customUserDetails);

        // Сохраняем токены в базу данных
        tokenService.saveToken(accessToken, Token.TokenType.ACCESS, user);
        tokenService.saveToken(refreshToken, Token.TokenType.REFRESH, user);

        // Возвращаем UserResponse с данными пользователя и токенами
        String avatarUrl = buildPresignedAvatarUrl(user);
        return UserResponse.builder().id(user.getId()).username(user.getUsername()).firstName(user.getFirstName()).lastName(user.getLastName()).patronymic(user.getPatronymic()).dateOfBirth(user.getDateOfBirth()).email(user.getEmail()).phone(user.getPhone()).avatarUrl(avatarUrl).emailVerified(user.isEmailVerified()).roles(user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet())).brands(user.getBrands().stream().map(brand -> BrandDto.builder().id(brand.getId()).name(brand.getName()).build()).collect(Collectors.toSet())).accessToken(accessToken).refreshToken(refreshToken).createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt()).build();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserProfile(String username) {
        User user = userService.findByUsername(username).orElseThrow(() -> new BadRequestException("Пользователь не найден"));

        String avatarUrl = buildPresignedAvatarUrl(user);
        return UserResponse.builder().id(user.getId()).username(user.getUsername()).firstName(user.getFirstName()).lastName(user.getLastName()).patronymic(user.getPatronymic()).dateOfBirth(user.getDateOfBirth()).email(user.getEmail()).phone(user.getPhone()).avatarUrl(avatarUrl).emailVerified(user.isEmailVerified()).roles(user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet())).brands(user.getBrands().stream().map(brand -> BrandDto.builder().id(brand.getId()).name(brand.getName()).build()).collect(Collectors.toSet())).createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt()).build();
    }

    private String buildPresignedAvatarUrl(User user) {
        try {
            var files = storageFileRepository.findByOwnerTypeAndOwnerId("USER", user.getId());
            var avatar = files.stream().filter(f -> "USER_AVATAR".equals(f.getPurpose())).findFirst();
            if (avatar.isPresent()) {
                return s3StorageService.buildPresignedGetUrl(avatar.get().getPath(), Duration.ofHours(12)).orElse(user.getAvatarUrl());
            }
            return user.getAvatarUrl();
        } catch (Exception e) {
            return user.getAvatarUrl();
        }
    }

    // Генерирует 6-значный код подтверждения email
    private String generateEmailCode() {
        int code = 100000 + random.nextInt(900000); // диапазон 100000-999999
        return String.valueOf(code);
    }

    // Generate an English lowercase slug suitable for subdomains
    private String generateSlugFromUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
    }
}
