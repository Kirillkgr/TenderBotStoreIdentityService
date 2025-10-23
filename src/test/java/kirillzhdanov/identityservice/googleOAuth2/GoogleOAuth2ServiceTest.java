package kirillzhdanov.identityservice.googleOAuth2;

import kirillzhdanov.identityservice.model.Token;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.security.JwtUtils;
import kirillzhdanov.identityservice.service.ProvisioningServiceOps;
import kirillzhdanov.identityservice.service.TokenService;
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

    @BeforeEach
    void setUp() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        tokenService = mock(TokenService.class);
        provisioningService = mock(ProvisioningServiceOps.class);
        linker = mock(OAuth2UserLinker.class);
        service = new GoogleOAuth2Service(jwtUtils, tokenService, provisioningService, linker);

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
        User created = User.builder().id(10L).username("user").build();
        when(linker.linkOrCreate(any())).thenReturn(new OAuth2UserLinker.Result(created, true));

        GoogleOAuth2Service.Tokens tokens = service.handleLoginOrRegister(oidcUser);

        // Проверяем, что токены выданы
        assertThat(tokens.accessToken()).isEqualTo("access.jwt");
        assertThat(tokens.refreshToken()).isEqualTo("refresh.jwt");

        // Проверяем, что токены сохранены
        verify(tokenService).saveToken(eq("access.jwt"), eq(Token.TokenType.ACCESS), any(User.class));
        verify(tokenService).saveToken(eq("refresh.jwt"), eq(Token.TokenType.REFRESH), any(User.class));

        // Провиженинг вызван (без автосоздания бренда)
        verify(provisioningService, atLeastOnce()).ensureMasterAccountForUser(any(User.class));
        verify(provisioningService, atLeastOnce()).ensureOwnerMembership(any(User.class), nullable(MasterAccount.class));
        verify(provisioningService, never()).ensureDefaultBrandAndPickup(any(User.class), any(MasterAccount.class));
    }

    @Test
    @DisplayName("Первый вход через Google вызывает полное провиженинг-окружение (master, membership, brand/pickup)")
    void firstGoogleLogin_invokesProvisioning() {
        OidcUser oidcUser = mock(OidcUser.class);
        when(linker.linkOrCreate(any())).thenReturn(new OAuth2UserLinker.Result(User.builder().id(100L).username("john").build(), true));

        service.handleLoginOrRegister(oidcUser);

        // Проверяем, что вызваны ensureMaster/ensureOwner, но НЕ создаётся бренд
        verify(provisioningService, atLeastOnce()).ensureMasterAccountForUser(any(User.class));
        verify(provisioningService, atLeastOnce()).ensureOwnerMembership(any(User.class), any(MasterAccount.class));
        verify(provisioningService, never()).ensureDefaultBrandAndPickup(any(User.class), any(MasterAccount.class));
    }
    // Avatar overwrite and email linking behavior now covered by DefaultOAuth2UserLinker tests.
}
