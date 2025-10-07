package kirillzhdanov.identityservice.googleOAuth2;

import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.Token;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.UserProvider;
import kirillzhdanov.identityservice.repository.UserProviderRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.security.JwtUtils;
import kirillzhdanov.identityservice.service.RoleService;
import kirillzhdanov.identityservice.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class GoogleOAuth2ServiceTest {

    private UserRepository userRepository;
    private TokenService tokenService;
    private UserProviderRepository userProviderRepository;
    private MasterAccountRepository masterAccountRepository;

    private GoogleOAuth2Service service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        RoleService roleService = mock(RoleService.class);
        JwtUtils jwtUtils = mock(JwtUtils.class);
        tokenService = mock(TokenService.class);
        userProviderRepository = mock(UserProviderRepository.class);
        masterAccountRepository = mock(MasterAccountRepository.class);
        // By default, pretend master already exists to avoid save path in unit tests
        when(masterAccountRepository.findByName(anyString())).thenReturn(java.util.Optional.of(new kirillzhdanov.identityservice.model.master.MasterAccount()));
        service = new GoogleOAuth2Service(userRepository, roleService, jwtUtils, tokenService, userProviderRepository, masterAccountRepository);

        Role userRole = new Role();
        userRole.setName(Role.RoleName.USER);
        when(roleService.getUserRole()).thenReturn(userRole);
        when(jwtUtils.generateAccessToken(any())).thenReturn("access.jwt");
        when(jwtUtils.generateRefreshToken(any())).thenReturn("refresh.jwt");
    }

    @Test
    @DisplayName("OAuth2: вход по уже привязанному провайдеру -> не создаёт пользователя, выдаёт токены")
    void loginWithExistingProviderMapping_returnsTokensWithoutCreatingUser() {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("linked@example.com");
        when(oidcUser.getSubject()).thenReturn("google-sub-linked");
        when(oidcUser.getGivenName()).thenReturn("Anna");
        when(oidcUser.getFamilyName()).thenReturn("Ivanova");
        when(oidcUser.getPicture()).thenReturn("https://example.com/pic.jpg");

        // Уже существует связь провайдер+providerUserId
        User user = User.builder().id(42L).username("linkedUser").roles(new java.util.HashSet<>()).build();
        UserProvider up = new UserProvider();
        up.setUser(user);
        when(userProviderRepository.findByProviderAndProviderUserId(eq(UserProvider.Provider.GOOGLE), anyString()))
                .thenReturn(Optional.of(up));

        GoogleOAuth2Service.Tokens tokens = service.handleLoginOrRegister(oidcUser);

        // Токены выданы
        assertThat(tokens.accessToken()).isEqualTo("access.jwt");
        assertThat(tokens.refreshToken()).isEqualTo("refresh.jwt");

        // Пользователь НЕ создавался
        verify(userRepository, never()).save(any(User.class));
        // Связь провайдера НЕ дублируется
        verify(userProviderRepository, never()).save(any(UserProvider.class));
        // Токены сохранены
        verify(tokenService).saveToken(eq("access.jwt"), eq(Token.TokenType.ACCESS), any(User.class));
        verify(tokenService).saveToken(eq("refresh.jwt"), eq(Token.TokenType.REFRESH), any(User.class));
    }

    @Test
    @DisplayName("Создание нового пользователя из OIDC + аватар и сохранение токенов")
    void createUserAndSaveTokens() {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("user@example.com");
        when(oidcUser.getSubject()).thenReturn("google-sub-1");
        when(oidcUser.getGivenName()).thenReturn("Ivan");
        when(oidcUser.getFamilyName()).thenReturn("Petrov");
        when(oidcUser.getPicture()).thenReturn("https://example.com/avatar.jpg");

        when(userProviderRepository.findByProviderAndProviderUserId(eq(UserProvider.Provider.GOOGLE), anyString()))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        // save returns user with id
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            if (u.getRoles() == null) u.setRoles(new HashSet<>());
            return u;
        });

        GoogleOAuth2Service.Tokens tokens = service.handleLoginOrRegister(oidcUser);

        // Проверяем, что токены выданы
        assertThat(tokens.accessToken()).isEqualTo("access.jwt");
        assertThat(tokens.refreshToken()).isEqualTo("refresh.jwt");

        // Проверяем, что пользователь сохранён с аватаром
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getUsername()).startsWith("user");
        assertThat(saved.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
        assertThat(saved.getEmail()).isEqualTo("user@example.com");

        // Проверяем, что токены сохранены
        verify(tokenService).saveToken(eq("access.jwt"), eq(Token.TokenType.ACCESS), any(User.class));
        verify(tokenService).saveToken(eq("refresh.jwt"), eq(Token.TokenType.REFRESH), any(User.class));

        // Проверяем, что связь с провайдером создана
        verify(userProviderRepository).save(any(UserProvider.class));
    }

    @Test
    @DisplayName("Существующий email — только линковка провайдера и обновление аватара, без дублирования")
    void linkExistingByEmailAndUpdateAvatarIfEmpty() {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("user@example.com");
        when(oidcUser.getSubject()).thenReturn("google-sub-2");
        when(oidcUser.getGivenName()).thenReturn("Ivan");
        when(oidcUser.getFamilyName()).thenReturn("Petrov");
        when(oidcUser.getPicture()).thenReturn("https://example.com/new-avatar.jpg");

        when(userProviderRepository.findByProviderAndProviderUserId(eq(UserProvider.Provider.GOOGLE), anyString()))
                .thenReturn(Optional.empty());
        User existing = User.builder().id(5L).username("user").email("user@example.com").avatarUrl("")
                .roles(new HashSet<>()).build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existing));

        GoogleOAuth2Service.Tokens tokens = service.handleLoginOrRegister(oidcUser);
        assertThat(tokens.accessToken()).isEqualTo("access.jwt");

        // Аватар должен обновиться
        verify(userRepository).save(argThat(u -> "https://example.com/new-avatar.jpg".equals(u.getAvatarUrl())));
        // Линковка провайдера
        verify(userProviderRepository).save(any(UserProvider.class));
    }
}
