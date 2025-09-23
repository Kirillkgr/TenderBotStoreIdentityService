package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.dto.*;
import kirillzhdanov.identityservice.exception.BadRequestException;
import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.exception.TokenRefreshException;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.Token;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
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
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;

	private final BrandRepository brandRepository;

	private final RoleService roleService;

	private final PasswordEncoder passwordEncoder;

	private final AuthenticationManager authenticationManager;

	private final JwtUtils jwtUtils;

	private final TokenService tokenService;

    private final SecureRandom random = new SecureRandom();


    @Transactional
    public boolean checkUniqUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional
    public UserResponse registerUser(UserRegistrationRequest request) {
        // Проверяем, что пользователь с таким именем не существует
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Пользователь с таким именем уже существует");
        }

        // Проверяем уникальность email, если он указан
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Пользователь с таким email уже существует");
            }
        }

        // Создаем нового пользователя
        User user = User.builder().username(request.getUsername()).password(passwordEncoder.encode(request.getPassword())).firstName(request.getFirstName()).lastName(request.getLastName()).patronymic(request.getPatronymic()).dateOfBirth(request.getDateOfBirth()).email(request.getEmail()).phone(request.getPhone()).emailVerified(false).brands(new HashSet<>()).roles(new HashSet<>()).build();

        // Добавляем роль USER по умолчанию
        Role userRole = roleService.getUserRole();
        user.getRoles().add(userRole);

        // Если указаны дополнительные роли, добавляем их
        if (request.getRoleNames() != null && !request.getRoleNames().isEmpty()) {
            for (Role.RoleName roleName : request.getRoleNames()) {
                roleService.findByName(roleName).ifPresent(role -> {
                    if (!role.getName().equals(Role.RoleName.USER)) { // USER уже добавлен
                        user.getRoles().add(role);
                    }
                });
            }
        }

        // Добавляем бренды, если они указаны
        if (request.getBrandIds() != null && !request.getBrandIds().isEmpty()) {
            Set<Brand> brands = new HashSet<>();
            for (Long brandId : request.getBrandIds()) {
                Brand brand = brandRepository.findById(brandId).orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + brandId));
                brands.add(brand);
            }
            user.setBrands(brands);
        }

        // Генерируем код подтверждения email, если email указан
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            user.setEmailVerificationCode(generateEmailCode());
            user.setEmailVerificationExpiresAt(LocalDateTime.now().plusMinutes(15));
        }

        // Сохраняем пользователя
        User savedUser = userRepository.save(user);

        // Создаем CustomUserDetails для включения дополнительной информации в токен
        CustomUserDetails customUserDetails = new CustomUserDetails(savedUser);

        // Генерируем токены
        String accessToken = jwtUtils.generateAccessToken(customUserDetails);
        String refreshToken = jwtUtils.generateRefreshToken(customUserDetails);

        // Сохраняем токены в базу данных
        tokenService.saveToken(accessToken, Token.TokenType.ACCESS, user);
        tokenService.saveToken(refreshToken, Token.TokenType.REFRESH, user);

        // Возвращаем ответ
        return UserResponse.builder().id(savedUser.getId()).username(savedUser.getUsername()).firstName(savedUser.getFirstName()).lastName(savedUser.getLastName()).patronymic(savedUser.getPatronymic()).dateOfBirth(savedUser.getDateOfBirth()).email(savedUser.getEmail()).phone(savedUser.getPhone()).emailVerified(savedUser.isEmailVerified()).roles(savedUser.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet())).brands(savedUser.getBrands()
                        .stream()
                        .map(brand -> BrandDto.builder()
                                .id(brand.getId())
                                .name(brand.getName())
                                .build())
                        .collect(Collectors.toSet()))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
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
        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Transactional
    public void revokeToken(String token) {
        tokenService.revokeToken(token);
    }

    @Transactional
    public void revokeAllUserTokens(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Пользователь не найден"));
        tokenService.revokeAllUserTokens(user);
    }

    @Transactional
    public UserResponse login(LoginRequest request) {
        // Аутентифицируем пользователя
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        // Получаем данные пользователя
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow(() -> new BadRequestException("Пользователь не найден"));

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
        return UserResponse.builder().id(user.getId()).username(user.getUsername()).firstName(user.getFirstName()).lastName(user.getLastName()).patronymic(user.getPatronymic()).dateOfBirth(user.getDateOfBirth()).email(user.getEmail()).phone(user.getPhone()).emailVerified(user.isEmailVerified()).roles(user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet())).brands(user.getBrands().stream().map(brand -> BrandDto.builder().id(brand.getId()).name(brand.getName()).build()).collect(Collectors.toSet())).accessToken(accessToken).refreshToken(refreshToken).build();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Пользователь не найден"));

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .patronymic(user.getPatronymic())
                .dateOfBirth(user.getDateOfBirth())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .emailVerified(user.isEmailVerified())
                .roles(user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet()))
                .brands(user.getBrands().stream().map(brand -> BrandDto.builder().id(brand.getId()).name(brand.getName()).build()).collect(Collectors.toSet()))
                .build();
    }

    // Генерирует 6-значный код подтверждения email
    private String generateEmailCode() {
        int code = 100000 + random.nextInt(900000); // диапазон 100000-999999
        return String.valueOf(code);
    }
}
