package kirillzhdanov.identityservice.security;

import jakarta.servlet.http.HttpServletRequest;
import kirillzhdanov.identityservice.model.Token;
import kirillzhdanov.identityservice.service.TokenService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Objects;

/**
 * Класс для аутентификации пользователя с использованием JWT токена
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticator {

	private final JwtUtils jwtUtils;

	private final UserDetailsService userDetailsService;

	private final TokenService tokenService;

	/**
	 * Обрабатывает JWT токен, проверяя его тип и аутентифицируя пользователя
	 *
	 * @param request HTTP запрос
	 * @param jwt     JWT токен
	 *
	 * @return true, если аутентификация прошла успешно
	 */
	public boolean processJwtToken(@NonNull HttpServletRequest request, @NonNull String jwt){
		// Проверяем тип токена - должен быть только ACCESS
		Token.TokenType tokenType;
		try {
			tokenType = jwtUtils.extractTokenType(jwt);
			if(tokenType != Token.TokenType.ACCESS) {
				log.warn("Неверный тип токена: {}", tokenType);
				return false;
			}
		} catch(Exception e) {
			log.warn("Не удалось извлечь тип токена: {}", e.getClass().getSimpleName());
			return false;
		}

		// Извлекаем имя пользователя из токена
		String username;
		try {
			username = jwtUtils.extractUsername(jwt);
			if(username == null || username.isEmpty()) {
				log.warn("Не удалось извлечь имя пользователя из токена");
				return false;
			}
		} catch(Exception e) {
			log.warn("Ошибка при извлечении имени пользователя: {}", e.getClass().getSimpleName());
			return false;
		}

		// Если пользователь еще не аутентифицирован
		if(SecurityContextHolder.getContext().getAuthentication() == null) {
			return authenticateUser(request, jwt, username);
		}

		return true;
	}

	/**
	 * Аутентифицирует пользователя, если токен валиден
	 *
	 * @param request  HTTP запрос
	 * @param jwt      JWT токен
	 * @param username имя пользователя
	 *
	 * @return true, если аутентификация прошла успешно
	 */
	private boolean authenticateUser(@NonNull HttpServletRequest request, @NonNull String jwt, @NonNull String username){

		try {
			UserDetails userDetails = userDetailsService.loadUserByUsername(username);
			Objects.requireNonNull(userDetails, "UserDetailsService вернул null");

			// Проверяем валидность токена в JWT и в базе данных
			if(isTokenValid(jwt, userDetails)) {
				UsernamePasswordAuthenticationToken authToken = createAuthenticationToken(userDetails);
				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authToken);

				if(log.isDebugEnabled()) {
					log.debug("Пользователь успешно аутентифицирован");
				}
				return true;
			} else {
				log.warn("Токен недействителен для пользователя");
				clearSecurityContext();
				return false;
			}
		} catch(UsernameNotFoundException e) {
			log.warn("Пользователь не найден");
			clearSecurityContext();
			return false;
		} catch(NullPointerException e) {
			log.warn("UserDetailsService вернул null");
			clearSecurityContext();
			return false;
		} catch(Exception e) {
			log.error("Ошибка при аутентификации пользователя: {}", e.getClass().getSimpleName());
			clearSecurityContext();
			return false;
		}
	}

	/**
	 * Создает токен аутентификации
	 *
	 * @param userDetails данные пользователя
	 *
	 * @return токен аутентификации
	 */
	private UsernamePasswordAuthenticationToken createAuthenticationToken(@NonNull UserDetails userDetails){

		return new UsernamePasswordAuthenticationToken(
				userDetails,
				null,
				userDetails.getAuthorities() != null ? userDetails.getAuthorities() : Collections.emptyList()
		);
	}

	/**
	 * Проверяет валидность токена
	 *
	 * @param jwt         JWT токен
	 * @param userDetails данные пользователя
	 *
	 * @return true, если токен валиден
	 */
	public boolean isTokenValid(@NonNull String jwt, @NonNull UserDetails userDetails){

		boolean isJwtValid;

		try {
			isJwtValid = jwtUtils.validateToken(jwt, userDetails);
			if(!isJwtValid) {
				return false;
			}
		} catch(Exception e) {
			log.warn("Ошибка при проверке JWT токена: {}", e.getClass().getSimpleName());
			return false;
		}

		try {
			return tokenService.isTokenValid(jwt);
		} catch(Exception e) {
			log.warn("Ошибка при проверке токена в базе данных: {}", e.getClass().getSimpleName());
			return false;
		}
	}

	/**
	 * Очищает контекст безопасности
	 */
	public void clearSecurityContext(){

		SecurityContextHolder.clearContext();
	}
}
