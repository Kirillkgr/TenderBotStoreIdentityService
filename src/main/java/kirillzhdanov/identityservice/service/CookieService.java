package kirillzhdanov.identityservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class CookieService {

    @Value("${server.servlet.session.cookie.domain:}")
    private String cookieDomain;

    @Value("${jwt.refresh.expiration:2592000000}")
    private long refreshExpirationMs;

    // Allow disabling Secure in dev environments where backend runs over HTTP
    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    public ResponseCookie buildRefreshCookie(String refreshToken) {
        ResponseCookie.ResponseCookieBuilder rcb = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("None");
        if (refreshExpirationMs > 0) {
            long seconds = Math.max(1, refreshExpirationMs / 1000);
            rcb.maxAge(Duration.ofSeconds(seconds));
        }
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            rcb.domain(cookieDomain);
        }
        return rcb.build();
    }

    public List<ResponseCookie> buildClearRefreshCookies() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("None");
        ResponseCookie withDomain = (cookieDomain != null && !cookieDomain.isBlank())
                ? builder.domain(cookieDomain).build()
                : builder.build();
        // Host-only variant without Domain
        ResponseCookie noDomain = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("None")
                .build();
        return List.of(withDomain, noDomain);
    }

    public List<ResponseCookie> buildClearSessionCookies() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("JSESSIONID", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("None");
        ResponseCookie withDomain = (cookieDomain != null && !cookieDomain.isBlank())
                ? builder.domain(cookieDomain).build()
                : builder.build();
        ResponseCookie noDomain = ResponseCookie.from("JSESSIONID", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("None")
                .build();
        return List.of(withDomain, noDomain);
    }
}
