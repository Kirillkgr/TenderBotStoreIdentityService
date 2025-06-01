package kirillzhdanov.identityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kirillzhdanov.identityservice.dto.BrandDto;
import kirillzhdanov.identityservice.dto.LoginRequest;
import kirillzhdanov.identityservice.dto.TokenRefreshRequest;
import kirillzhdanov.identityservice.dto.TokenRefreshResponse;
import kirillzhdanov.identityservice.dto.UserRegistrationRequest;
import kirillzhdanov.identityservice.dto.UserResponse;
import kirillzhdanov.identityservice.exception.BadRequestException;
import kirillzhdanov.identityservice.exception.TokenRefreshException;
import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AuthService authService;

	private UserRegistrationRequest registrationRequest;

	private LoginRequest loginRequest;

	private TokenRefreshRequest refreshRequest;

	private UserResponse userResponse;

	private TokenRefreshResponse refreshResponse;

	@BeforeEach
	void setUp(){
		// Подготовка данных для тестов
		registrationRequest = new UserRegistrationRequest();
		registrationRequest.setUsername("testuser");
		registrationRequest.setPassword("Password123!");
		Set<Role.RoleName> roles = new HashSet<>();
		roles.add(Role.RoleName.USER);
		registrationRequest.setRoleNames(roles);

		loginRequest = new LoginRequest();
		loginRequest.setUsername("testuser");
		loginRequest.setPassword("Password123!");

		refreshRequest = new TokenRefreshRequest();
		refreshRequest.setRefreshToken("refresh-token-123");

		Set<String> roleStrings = new HashSet<>();
		roleStrings.add("USER");
		Set<BrandDto> brands = new HashSet<>();

		userResponse = UserResponse.builder()
							   .id(1L)
							   .username("testuser")
							   .roles(roleStrings)
							   .brands(brands)
							   .accessToken("access-token-123")
							   .refreshToken("refresh-token-123")
							   .build();

		refreshResponse = TokenRefreshResponse.builder()
								  .accessToken("new-access-token-123")
								  .refreshToken("refresh-token-123")
								  .build();
	}

	@Test
	@DisplayName("Регистрация пользователя - успешно")
	void registerUser_Success() throws Exception{

		when(authService.registerUser(any(UserRegistrationRequest.class))).thenReturn(userResponse);

		mockMvc.perform(post("/auth/register")
								.contentType(MediaType.APPLICATION_JSON)
								.content(objectMapper.writeValueAsString(registrationRequest)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username", is("testuser")))
				.andExpect(jsonPath("$.accessToken", is("access-token-123")))
				.andExpect(jsonPath("$.refreshToken", is("refresh-token-123")));

		verify(authService, times(1)).registerUser(any(UserRegistrationRequest.class));
	}

	@Test
	@DisplayName("Регистрация пользователя - имя пользователя уже существует")
	void registerUser_UsernameAlreadyExists() throws Exception{

		when(authService.registerUser(any(UserRegistrationRequest.class)))
				.thenThrow(new BadRequestException("Пользователь с таким именем уже существует"));

		mockMvc.perform(post("/auth/register")
								.contentType(MediaType.APPLICATION_JSON)
								.content(objectMapper.writeValueAsString(registrationRequest)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Пользователь с таким именем уже существует")));

		verify(authService, times(1)).registerUser(any(UserRegistrationRequest.class));
	}

	@Test
	@DisplayName("Регистрация пользователя - невалидные данные")
	void registerUser_InvalidData() throws Exception{
		// Создаем запрос с невалидными данными
		UserRegistrationRequest invalidRequest = new UserRegistrationRequest();
		invalidRequest.setUsername(""); // Пустое имя пользователя
		invalidRequest.setPassword("123"); // Слишком короткий пароль

		mockMvc.perform(post("/auth/register")
								.contentType(MediaType.APPLICATION_JSON)
								.content(objectMapper.writeValueAsString(invalidRequest)))
				.andExpect(status().isBadRequest());

		verify(authService, never()).registerUser(any(UserRegistrationRequest.class));
	}

	@Test
	@DisplayName("Вход пользователя - успешно")
	void login_Success() throws Exception{

		when(authService.login(any(LoginRequest.class))).thenReturn(userResponse);

		mockMvc.perform(post("/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(objectMapper.writeValueAsString(loginRequest)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username", is("testuser")))
				.andExpect(jsonPath("$.accessToken", is("access-token-123")))
				.andExpect(jsonPath("$.refreshToken", is("refresh-token-123")));

		verify(authService, times(1)).login(any(LoginRequest.class));
	}

	@Test
	@DisplayName("Вход пользователя - неверные учетные данные")
	void login_InvalidCredentials() throws Exception{

		when(authService.login(any(LoginRequest.class)))
				.thenThrow(new BadRequestException("Неверные учетные данные"));

		mockMvc.perform(post("/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(objectMapper.writeValueAsString(loginRequest)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Неверные учетные данные")));

		verify(authService, times(1)).login(any(LoginRequest.class));
	}

	@Test
	@DisplayName("Обновление токена - успешно")
	void refreshToken_Success() throws Exception{

		when(authService.refreshToken(any(TokenRefreshRequest.class))).thenReturn(refreshResponse);

		mockMvc.perform(post("/auth/refresh")
								.contentType(MediaType.APPLICATION_JSON)
								.content(objectMapper.writeValueAsString(refreshRequest)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken", is("new-access-token-123")))
				.andExpect(jsonPath("$.refreshToken", is("refresh-token-123")));

		verify(authService, times(1)).refreshToken(any(TokenRefreshRequest.class));
	}

	@Test
	@DisplayName("Обновление токена - недействительный токен")
	void refreshToken_InvalidToken() throws Exception{

		when(authService.refreshToken(any(TokenRefreshRequest.class)))
				.thenThrow(new TokenRefreshException("Токен обновления недействителен или истек"));

		mockMvc.perform(post("/auth/refresh")
								.contentType(MediaType.APPLICATION_JSON)
								.content(objectMapper.writeValueAsString(refreshRequest)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message", is("Токен обновления недействителен или истек")));

		verify(authService, times(1)).refreshToken(any(TokenRefreshRequest.class));
	}

	@Test
	@DisplayName("Отзыв токена - успешно")
	@WithMockUser(roles = "USER")
	void revokeToken_Success() throws Exception{

		doNothing().when(authService).revokeToken(anyString());

		mockMvc.perform(post("/auth/revoke")
								.param("token", "access-token-123"))
				.andExpect(status().isOk());

		verify(authService, times(1)).revokeToken("access-token-123");
	}

	@Test
	@DisplayName("Отзыв токена - без аутентификации")
	void revokeToken_Unauthorized() throws Exception{

		mockMvc.perform(post("/auth/revoke")
								.param("token", "access-token-123"))
				.andExpect(status().isUnauthorized());

		verify(authService, never()).revokeToken(anyString());
	}

	@Test
	@DisplayName("Отзыв всех токенов пользователя - успешно")
	@WithMockUser(roles = "ADMIN")
	void revokeAllUserTokens_Success() throws Exception{

		doNothing().when(authService).revokeAllUserTokens(anyString());

		mockMvc.perform(post("/auth/revoke-all")
								.param("username", "testuser"))
				.andExpect(status().isOk());

		verify(authService, times(1)).revokeAllUserTokens("testuser");
	}

	@Test
	@DisplayName("Отзыв всех токенов пользователя - без прав администратора")
	@WithMockUser(roles = "USER")
	void revokeAllUserTokens_Forbidden() throws Exception{

		mockMvc.perform(post("/auth/revoke-all")
								.param("username", "testuser"))
				.andExpect(status().isForbidden());

		verify(authService, never()).revokeAllUserTokens(anyString());
	}

	@Test
	@DisplayName("Выход из системы - успешно")
	void logout_Success() throws Exception{

		doNothing().when(authService).revokeToken(anyString());

		mockMvc.perform(delete("/auth/logout")
								.contentType(MediaType.TEXT_PLAIN)
								.content("access-token-123"))
				.andExpect(status().isOk());

		verify(authService, times(1)).revokeToken("access-token-123");
	}

	@Test
	@DisplayName("Выход из всех сессий - успешно")
	void logoutAll_Success() throws Exception{

		doNothing().when(authService).revokeAllUserTokens(anyString());

		mockMvc.perform(delete("/auth/logout/all/{username}", "testuser"))
				.andExpect(status().isOk());

		verify(authService, times(1)).revokeAllUserTokens("testuser");
	}
}
