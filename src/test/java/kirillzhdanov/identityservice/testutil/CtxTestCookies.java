package kirillzhdanov.identityservice.testutil;

import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.util.HmacSigner;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Утилита для генерации подписанной cookie "ctx" в тестах.
 */
public final class CtxTestCookies {
    private CtxTestCookies() {
    }

    public static Cookie createCtx(Long masterId, Long brandId, Long pickupPointId, String secret) {
        long issuedAt = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        if (masterId != null) {
            sb.append("\"masterId\":").append(masterId);
            first = false;
        }
        if (brandId != null) {
            if (!first) sb.append(",");
            sb.append("\"brandId\":").append(brandId);
            first = false;
        }
        if (pickupPointId != null) {
            if (!first) sb.append(",");
            sb.append("\"pickupPointId\":").append(pickupPointId);
            first = false;
        }
        if (!first) sb.append(",");
        sb.append("\"issuedAt\":").append(issuedAt);
        sb.append("}");
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8));
        HmacSigner signer = new HmacSigner(secret != null ? secret : "change-me");
        String sig = signer.signBase64Url(payload);
        String value = payload + "." + sig;
        Cookie cookie = new Cookie("ctx", value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }
}
