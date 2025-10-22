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
import kirillzhdanov.identityservice.repository.StorageFileRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import kirillzhdanov.identityservice.repository.pickup.PickupPointRepository;
import kirillzhdanov.identityservice.security.CustomUserDetails;
import kirillzhdanov.identityservice.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserService userRepository;

    @Mock
    private BrandRepository brandRepository;
    @Mock
    MasterAccountRepository masterAccountRepository;
    @Mock
    PickupPointRepository pickupPointRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private TokenService tokenService;
    @Mock
    private UserMembershipRepository userMembershipRepository;
    @Mock
    private StorageFileRepository storageFileRepository;
    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private ProvisioningServiceOps provisioningService;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    private Role userRole;

    private Role adminRole;

    private UserRegistrationRequest registrationRequest;

    private LoginRequest loginRequest;

    private TokenRefreshRequest refreshRequest;

    private Token refreshToken;

    @BeforeEach
    void setUp() {
        // Подготовка тестовых данных
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName(Role.RoleName.USER);

        adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName(Role.RoleName.ADMIN);

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .brands(new HashSet<>())
                .build();

        registrationRequest = new UserRegistrationRequest();
        registrationRequest.setUsername("newuser");
        registrationRequest.setPassword("Password123!");
        Set<Role.RoleName> roles = new HashSet<>();
        roles.add(Role.RoleName.USER);
        registrationRequest.setRoleNames(roles);

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Password123!");

        refreshRequest = new TokenRefreshRequest();
        refreshRequest.setRefreshToken("refresh-token-123");

        refreshToken = Token.builder()
                .id(1L)
                .token("refresh-token-123")
                .tokenType(Token.TokenType.REFRESH)
                .revoked(false)
                .expiryDate(LocalDateTime.now()
                        .plusDays(7))
                .user(testUser)
                .build();
    }

    @Test
    @DisplayName("Регистрация пользователя - успешно")
    void registerUser_Success() {
        // Подготовка
        // провиженинг через интерфейс
        kirillzhdanov.identityservice.model.master.MasterAccount master = new kirillzhdanov.identityservice.model.master.MasterAccount();
        try {
            var f = master.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(master, 1L);
        } catch (Exception ignored) {}
        when(provisioningService.ensureMasterAccountForUser(any(User.class))).thenReturn(master);
        doNothing().when(provisioningService).ensureOwnerMembership(any(User.class), any());
        doNothing().when(provisioningService).ensureDefaultBrandAndPickup(any(User.class), any());
        when(storageFileRepository.findByOwnerTypeAndOwnerId(anyString(), anyLong())).thenReturn(Collections.emptyList());
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(roleService.getUserRole()).thenReturn(userRole);
        when(roleService.findByName(Role.RoleName.USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtils.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token-123");
        when(jwtUtils.generateRefreshToken(any(CustomUserDetails.class))).thenReturn("refresh-token-123");
        doNothing().when(tokenService)
                .saveToken(anyString(), any(Token.TokenType.class), any(User.class));

        // Выполнение
        UserResponse response = authService.registerUser(registrationRequest);

        // Проверка
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getId());
        assertEquals(testUser.getUsername(), response.getUsername());
        assertEquals("access-token-123", response.getAccessToken());
        assertEquals("refresh-token-123", response.getRefreshToken());

        verify(userRepository).existsByUsername(registrationRequest.getUsername());
        verify(roleService).getUserRole();
        verify(roleService).findByName(Role.RoleName.USER);
        verify(passwordEncoder).encode(registrationRequest.getPassword());
        // Сохранение пользователя как минимум один раз
        verify(userRepository, atLeastOnce()).save(any(User.class));
        verify(jwtUtils).generateAccessToken(any(CustomUserDetails.class));
        verify(jwtUtils).generateRefreshToken(any(CustomUserDetails.class));
        verify(tokenService).saveToken(eq("access-token-123"), eq(Token.TokenType.ACCESS), any(User.class));
        verify(tokenService).saveToken(eq("refresh-token-123"), eq(Token.TokenType.REFRESH), any(User.class));
    }

    @Test
    @DisplayName("Регистрация пользователя - имя пользователя уже существует")
    void registerUser_UsernameAlreadyExists() {
        // Подготовка
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // Выполнение и проверка
        assertThrows(BadRequestException.class, () -> authService.registerUser(registrationRequest));

        verify(userRepository).existsByUsername(registrationRequest.getUsername());
        verify(userRepository, never()).save(any(User.class));
    }
    @Test
    @DisplayName("Регистрация пользователя с дополнительными ролями - успешно")
    void registerUser_WithAdditionalRoles_Success() {
        // Подготовка
        registrationRequest.getRoleNames()
                .add(Role.RoleName.ADMIN);

        // провиженинг через интерфейс
        kirillzhdanov.identityservice.model.master.MasterAccount master = new kirillzhdanov.identityservice.model.master.MasterAccount();
        try { var f = master.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(master, 1L);} catch (Exception ignored) {}
        when(provisioningService.ensureMasterAccountForUser(any(User.class))).thenReturn(master);
        doNothing().when(provisioningService).ensureOwnerMembership(any(User.class), any());
        doNothing().when(provisioningService).ensureDefaultBrandAndPickup(any(User.class), any());
        when(storageFileRepository.findByOwnerTypeAndOwnerId(anyString(), anyLong())).thenReturn(Collections.emptyList());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        User userWithAdminRole = User.builder()
                .id(1L)
                .username("newuser")
                .password("encodedPassword")
                .roles(new HashSet<>(Arrays.asList(userRole, adminRole)))
                .brands(new HashSet<>())
                .build();

        when(userRepository.save(any(User.class))).thenReturn(userWithAdminRole);
        when(jwtUtils.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token-123");
        when(jwtUtils.generateRefreshToken(any(CustomUserDetails.class))).thenReturn("refresh-token-123");
        doNothing().when(tokenService)
                .saveToken(anyString(), any(Token.TokenType.class), any(User.class));

        // Выполнение
        UserResponse response = authService.registerUser(registrationRequest);

        // Проверка
        assertNotNull(response);
        verify(roleService).findByName(Role.RoleName.ADMIN);
    }

    @Test
    @DisplayName("Регистрация пользователя с брендами - успешно")
    void registerUser_WithBrands_Success() {
        // Подготовка
        Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("TestBrand");

        registrationRequest.setBrandIds(Collections.singleton(1L));

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(roleService.getUserRole()).thenReturn(userRole);
        when(roleService.findByName(Role.RoleName.USER)).thenReturn(Optional.of(userRole));
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        // провиженинг через интерфейс
        kirillzhdanov.identityservice.model.master.MasterAccount master = new kirillzhdanov.identityservice.model.master.MasterAccount();
        try { var f = master.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(master, 1L);} catch (Exception ignored) {}
        when(provisioningService.ensureMasterAccountForUser(any(User.class))).thenReturn(master);
        doNothing().when(provisioningService).ensureOwnerMembership(any(User.class), any());
        doNothing().when(provisioningService).ensureDefaultBrandAndPickup(any(User.class), any());

        User userWithBrand = User.builder()
                .id(1L)
                .username("newuser")
                .password("encodedPassword")
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .brands(new HashSet<>(Collections.singletonList(brand)))
                .build();

        when(userRepository.save(any(User.class))).thenReturn(userWithBrand);
        when(jwtUtils.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token-123");
        when(jwtUtils.generateRefreshToken(any(CustomUserDetails.class))).thenReturn("refresh-token-123");
        doNothing().when(tokenService)
                .saveToken(anyString(), any(Token.TokenType.class), any(User.class));

        // Выполнение
        UserResponse response = authService.registerUser(registrationRequest);

        // Проверка
        assertNotNull(response);
        verify(brandRepository).findById(1L);
    }

    @Test
    @DisplayName("Регистрация пользователя - бренд не найден")
    void registerUser_BrandNotFound() {
        // Подготовка
        registrationRequest.setBrandIds(Collections.singleton(999L));

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(roleService.getUserRole()).thenReturn(userRole);
        when(roleService.findByName(Role.RoleName.USER)).thenReturn(Optional.of(userRole));
        when(brandRepository.findById(999L)).thenReturn(Optional.empty());

        // Выполнение и проверка
        assertThrows(ResourceNotFoundException.class, () -> authService.registerUser(registrationRequest));

        verify(brandRepository).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Вход пользователя - успешно")
    void login_Success() {
        // Подготовка
        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token-123");
        when(jwtUtils.generateRefreshToken(any(CustomUserDetails.class))).thenReturn("refresh-token-123");
        doNothing().when(tokenService)
                .revokeAllUserTokens(any(User.class));
        doNothing().when(tokenService)
                .saveToken(anyString(), any(Token.TokenType.class), any(User.class));

        // Выполнение
        UserResponse response = authService.login(loginRequest);

        // Проверка
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getId());
        assertEquals(testUser.getUsername(), response.getUsername());
        assertEquals("access-token-123", response.getAccessToken());
        assertEquals("refresh-token-123", response.getRefreshToken());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("testuser");
        verify(tokenService).revokeAllUserTokens(testUser);
        verify(jwtUtils).generateAccessToken(any(CustomUserDetails.class));
        verify(jwtUtils).generateRefreshToken(any(CustomUserDetails.class));
        verify(tokenService).saveToken(eq("access-token-123"), eq(Token.TokenType.ACCESS), eq(testUser));
        verify(tokenService).saveToken(eq("refresh-token-123"), eq(Token.TokenType.REFRESH), eq(testUser));
    }

    @Test
    @DisplayName("Вход пользователя - пользователь не найден")
    void login_UserNotFound() {
        // Подготовка
        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // Выполнение и проверка
        assertThrows(BadRequestException.class, () -> authService.login(loginRequest));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("testuser");
        verify(tokenService, never()).revokeAllUserTokens(any(User.class));
    }

    @Test
    @DisplayName("Обновление токена - успешно")
    void refreshToken_Success() {
        // Подготовка
        when(tokenService.findByToken("refresh-token-123")).thenReturn(Optional.of(refreshToken));
        when(jwtUtils.generateAccessToken(any(CustomUserDetails.class))).thenReturn("new-access-token-123");
        when(jwtUtils.generateRefreshToken(any(CustomUserDetails.class))).thenReturn("new-refresh-token-456");
        doNothing().when(tokenService).revokeToken(anyString());
        doNothing().when(tokenService).saveToken(anyString(), any(Token.TokenType.class), any(User.class));

        // Выполнение
        TokenRefreshResponse response = authService.refreshToken(refreshRequest);

        // Проверка
        assertNotNull(response);
        assertEquals("new-access-token-123", response.getAccessToken());
        assertEquals("new-refresh-token-456", response.getRefreshToken());

        // Старый refresh должен быть отозван (ротация)
        verify(tokenService).revokeToken("refresh-token-123");

        verify(tokenService).findByToken("refresh-token-123");
        verify(jwtUtils).generateAccessToken(any(CustomUserDetails.class));
        verify(jwtUtils).generateRefreshToken(any(CustomUserDetails.class));
        verify(tokenService).saveToken(eq("new-access-token-123"), eq(Token.TokenType.ACCESS), eq(testUser));
        verify(tokenService).saveToken(eq("new-refresh-token-456"), eq(Token.TokenType.REFRESH), eq(testUser));
    }

    @Test
    @DisplayName("Обновление токена - токен не найден")
    void refreshToken_TokenNotFound() {
        // Подготовка
        when(tokenService.findByToken("refresh-token-123")).thenReturn(Optional.empty());

        // Выполнение и проверка
        assertThrows(TokenRefreshException.class, () -> authService.refreshToken(refreshRequest));

        verify(tokenService).findByToken("refresh-token-123");
        verify(jwtUtils, never()).generateAccessToken(any(CustomUserDetails.class));
    }

    @Test
    @DisplayName("Обновление токена - недействительный токен")
    void refreshToken_InvalidToken() {
        // Подготовка
        Token invalidToken = Token.builder()
                .id(1L)
                .token("refresh-token-123")
                .tokenType(Token.TokenType.REFRESH)
                .revoked(true) // Отозванный токен
                .expiryDate(LocalDateTime.now()
                        .plusDays(7))
                .user(testUser)
                .build();

        when(tokenService.findByToken("refresh-token-123")).thenReturn(Optional.of(invalidToken));

        // Выполнение и проверка
        assertThrows(TokenRefreshException.class, () -> authService.refreshToken(refreshRequest));

        verify(tokenService).findByToken("refresh-token-123");
        verify(jwtUtils, never()).generateAccessToken(any(CustomUserDetails.class));
    }

    @Test
    @DisplayName("Обновление токена - неверный тип токена")
    void refreshToken_WrongTokenType() {
        // Подготовка
        Token accessToken = Token.builder()
                .id(1L)
                .token("refresh-token-123")
                .tokenType(Token.TokenType.ACCESS) // Неверный тип токена
                .revoked(false)
                .expiryDate(LocalDateTime.now()
                        .plusDays(7))
                .user(testUser)
                .build();

        when(tokenService.findByToken("refresh-token-123")).thenReturn(Optional.of(accessToken));

        // Выполнение и проверка
        assertThrows(TokenRefreshException.class, () -> authService.refreshToken(refreshRequest));

        verify(tokenService).findByToken("refresh-token-123");
        verify(jwtUtils, never()).generateAccessToken(any(CustomUserDetails.class));
    }

    @Test
    @DisplayName("Отзыв токена - успешно")
    void revokeToken_Success() {
        // Подготовка
        doNothing().when(tokenService)
                .revokeToken(anyString());

        // Выполнение
        authService.revokeToken("access-token-123");

        // Проверка
        verify(tokenService).revokeToken("access-token-123");
    }

    @Test
    @DisplayName("Отзыв всех токенов пользователя - успешно")
    void revokeAllUserTokens_Success() {
        // Подготовка
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        doNothing().when(tokenService)
                .revokeAllUserTokens(any(User.class));

        // Выполнение
        authService.revokeAllUserTokens("testuser");

        // Проверка
        verify(userRepository).findByUsername("testuser");
        verify(tokenService).revokeAllUserTokens(testUser);
    }

    @Test
    @DisplayName("Отзыв всех токенов пользователя - пользователь не найден")
    void revokeAllUserTokens_UserNotFound() {
        // Подготовка
        when(userRepository.findByUsername("nonexistentuser")).thenReturn(Optional.empty());

        // Выполнение и проверка
        assertThrows(BadRequestException.class, () -> authService.revokeAllUserTokens("nonexistentuser"));

        verify(userRepository).findByUsername("nonexistentuser");
        verify(tokenService, never()).revokeAllUserTokens(any(User.class));
    }
}
