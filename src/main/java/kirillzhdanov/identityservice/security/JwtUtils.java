package kirillzhdanov.identityservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.Token;
import kirillzhdanov.identityservice.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtUtils {

	@Value("${jwt.secret:secret_key_for_jwt_token_please_change_in_production}")
	private String secret;

	@Value("${jwt.access.expiration:3600000}")
	private Long accessTokenExpiration; // 1 час по умолчанию

	@Value("${jwt.refresh.expiration:2592000000}")
	private Long refreshTokenExpiration; // 30 дней по умолчанию

	public String extractUsername(String token){

		return extractClaim(token, Claims::getSubject);
	}

	public Date extractExpiration(String token){

		return extractClaim(token, Claims::getExpiration);
	}

	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver){

		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	private Claims extractAllClaims(String token){

		try {
			return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
		} catch(ExpiredJwtException e) {
			// Для истекших токенов все равно возвращаем claims для тестирования
			return e.getClaims();
		}
	}

	private Boolean isTokenExpired(String token){

		try {
			return extractExpiration(token).before(new Date());
		} catch(ExpiredJwtException e) {
			return true;
		}
	}

	public String generateAccessToken(UserDetails userDetails){

		Map<String, Object> claims = new HashMap<>();
		claims.put("tokenType", Token.TokenType.ACCESS.name());

		if(userDetails instanceof CustomUserDetails(User user)) {

			claims.put("userId", user.getId());
			claims.put("username", user.getUsername());

			List<Long> brandIds = user.getBrands().stream()
										  .map(Brand::getId)
										  .collect(Collectors.toList());
			claims.put("brandIds", brandIds);

			List<String> roles = user.getRoles().stream()
										 .map(role->role.getName().name())
										 .collect(Collectors.toList());
			claims.put("roles", roles);
		}

		return createToken(claims, userDetails.getUsername(), accessTokenExpiration);
	}

	public String generateRefreshToken(UserDetails userDetails){

		Map<String, Object> claims = new HashMap<>();
		claims.put("tokenType", Token.TokenType.REFRESH.name());
		return createToken(claims, userDetails.getUsername(), refreshTokenExpiration);
	}

	private String createToken(Map<String, Object> claims, String subject, Long expiration){

		return Jwts.builder()
					   .setClaims(claims)
					   .setSubject(subject)
					   .setIssuedAt(new Date(System.currentTimeMillis()))
					   .setExpiration(new Date(System.currentTimeMillis() + expiration))
					   .signWith(SignatureAlgorithm.HS256, secret)
					   .compact();
	}

	public Boolean validateToken(String token, UserDetails userDetails){

		final String username = extractUsername(token);
		return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
	}

	public Token.TokenType extractTokenType(String token){

		return Token.TokenType.valueOf(extractClaim(token, claims->claims.get("tokenType", String.class)));
	}

	public LocalDateTime extractExpirationAsLocalDateTime(String token){

		Date expirationDate = extractExpiration(token);
		return expirationDate.toInstant()
					   .atZone(ZoneId.systemDefault())
					   .toLocalDateTime();
	}

	public Long extractUserId(String token){

		return extractClaim(token, claims->{
			Object userId = claims.get("userId");
			return userId != null ? ((Number) userId).longValue() : null;
		});
	}

	@SuppressWarnings("unchecked")
	public List<Long> extractBrandIds(String token){

		return extractClaim(token, claims->{
			Object brandIds = claims.get("brandIds");
			return brandIds != null ? (List<Long>) brandIds : Collections.emptyList();
		});
	}

	@SuppressWarnings("unchecked")
	public List<String> extractRoles(String token){

		return extractClaim(token, claims->{
			Object roles = claims.get("roles");
			return roles != null ? (List<String>) roles : Collections.emptyList();
		});
	}

	/**
	 * Проверяет только валидность подписи и срока действия токена, без проверки пользователя
	 * @param token JWT токен для проверки
	 * @return true если токен имеет валидную подпись и не истек, иначе false
	 */
	public boolean validateTokenSignature(String token) {
		try {
			Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
			return !isTokenExpired(token);
		} catch (Exception e) {
			return false;
		}
	}
}
