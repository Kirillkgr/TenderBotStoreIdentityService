package kirillzhdanov.identityservice.googleOAuth2;

import kirillzhdanov.identityservice.model.Token;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.security.JwtUtils;
import kirillzhdanov.identityservice.service.ProvisioningServiceOps;
import kirillzhdanov.identityservice.service.TokenService;
import kirillzhdanov.identityservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;


public class GoogleOAuth2ServiceTest {

    private TokenService tokenService;
    private ProvisioningServiceOps provisioningService;
    private OAuth2UserLinker linker;
    private GoogleOAuth2Service service;
    private UserService userService;

    @BeforeEach
    void setUp() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        tokenService = mock(TokenService.class);
        provisioningService = mock(ProvisioningServiceOps.class);
        linker = mock(OAuth2UserLinker.class);
        userService = mock(UserService.class);
        service = new GoogleOAuth2Service(jwtUtils, tokenService, provisioningService, linker, userService);

        when(jwtUtils.generateAccessToken(any())).thenReturn("access.jwt");
        when(jwtUtils.generateRefreshToken(any())).thenReturn("refresh.jwt");
        when(provisioningService.ensureMasterAccountForUser(any(User.class))).thenReturn(new MasterAccount());
    }

    @Test
    @DisplayName("OAuth2: вход по уже привязанному провайдеру -> не создаёт пользователя, выдаёт токены")
    void loginWithExistingProviderMapping_returnsTokensWithoutCreatingUser() {
        OidcUser oidcUser = mock(OidcUser.class);
        User user = User.builder().id(42L).username("linkedUser").build();
        when(linker.linkOrCreate(any())).thenReturn(new OAuth2UserLinker.Result(user, false));

        GoogleOAuth2Service.Tokens tokens = service.handleLoginOrRegister(oidcUser);

        // Токены выданы
        assertThat(tokens.accessToken()).isEqualTo("access.jwt");
        assertThat(tokens.refreshToken()).isEqualTo("refresh.jwt");

        // Провиженинг не вызывается при повторном входе
        verify(provisioningService, never()).ensureDefaultBrandAndPickup(any(), any());
        verify(provisioningService, never()).ensureOwnerMembership(any(), any());
        // Токены сохранены
        verify(tokenService).saveToken(eq("access.jwt"), eq(Token.TokenType.ACCESS), any(User.class));
        verify(tokenService).saveToken(eq("refresh.jwt"), eq(Token.TokenType.REFRESH), any(User.class));
    }

    @Test
    @DisplayName("Создание нового пользователя из OIDC + аватар и сохранение токенов")
    void createUserAndSaveTokens() {
        OidcUser oidcUser = mock(OidcUser.class);
        User created = User.builder().id(10L).username("user").brands(new java.util.HashSet<>()).build();
        when(linker.linkOrCreate(any())).thenReturn(new OAuth2UserLinker.Result(created, true));
        // Эмулируем, что после провиженинга у пользователя появился бренд и он виден при перезагрузке
        Brand b = new Brand(); b.setId(100L); b.setName("testbrand");
        User reloaded = User.builder().id(10L).username("user").brands(new java.util.HashSet<>(java.util.List.of(b))).build();
        when(userService.findByUsername(eq("user"))).thenReturn(java.util.Optional.of(reloaded));

        GoogleOAuth2Service.Tokens tokens = service.handleLoginOrRegister(oidcUser);

        // Проверяем, что токены выданы
        assertThat(tokens.accessToken()).isEqualTo("access.jwt");
        assertThat(tokens.refreshToken()).isEqualTo("refresh.jwt");

        // Проверяем, что токены сохранены ДЛЯ перезагруженного пользователя, у которого уже есть бренд
        org.mockito.ArgumentCaptor<User> userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(tokenService).saveToken(eq("access.jwt"), eq(Token.TokenType.ACCESS), userCaptor.capture());
        User usedForAccess = userCaptor.getValue();
        assertThat(usedForAccess.getUsername()).isEqualTo("user");
        assertThat(usedForAccess.getBrands()).isNotNull();
        assertThat(usedForAccess.getBrands()).hasSize(1);
        verify(tokenService).saveToken(eq("refresh.jwt"), eq(Token.TokenType.REFRESH), any(User.class));

        // Провиженинг вызван (включая автосоздание бренда) и пользователь перезагружается
        verify(provisioningService, atLeastOnce()).ensureMasterAccountForUser(any(User.class));
        verify(provisioningService, atLeastOnce()).ensureOwnerMembership(any(User.class), nullable(MasterAccount.class));
        verify(provisioningService, atLeastOnce()).ensureDefaultBrandAndPickup(any(User.class), nullable(MasterAccount.class));
        verify(userService, atLeastOnce()).findByUsername(eq("user"));
    }

    @Test
    @DisplayName("Первый вход через Google вызывает provisioning (master, membership, brand/pickup) и перезагрузку пользователя")
    void firstGoogleLogin_invokesProvisioning() {
        OidcUser oidcUser = mock(OidcUser.class);
        User john = User.builder().id(100L).username("john").build();
        when(linker.linkOrCreate(any())).thenReturn(new OAuth2UserLinker.Result(john, true));
        when(userService.findByUsername(eq("john"))).thenReturn(java.util.Optional.of(john));

        service.handleLoginOrRegister(oidcUser);

        // Проверяем, что вызваны ensureMaster/ensureOwner/ensureDefaultBrand и была перезагрузка пользователя
        verify(provisioningService, atLeastOnce()).ensureMasterAccountForUser(any(User.class));
        verify(provisioningService, atLeastOnce()).ensureOwnerMembership(any(User.class), any(MasterAccount.class));
        verify(provisioningService, atLeastOnce()).ensureDefaultBrandAndPickup(any(User.class), any(MasterAccount.class));
        verify(userService, atLeastOnce()).findByUsername(eq("john"));
    }
    // Avatar overwrite and email linking behavior now covered by DefaultOAuth2UserLinker tests.
}
