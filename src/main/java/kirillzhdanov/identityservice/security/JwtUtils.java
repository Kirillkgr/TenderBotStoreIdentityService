package kirillzhdanov.identityservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import kirillzhdanov.identityservice.model.Token;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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

		return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
	}

	private Boolean isTokenExpired(String token){

		return extractExpiration(token).before(new Date());
	}

	public String generateAccessToken(UserDetails userDetails){

		Map<String, Object> claims = new HashMap<>();
		claims.put("tokenType", Token.TokenType.ACCESS.name());
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
}
