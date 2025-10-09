package kirillzhdanov.identityservice.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HmacSigner {
    private final byte[] key;
    private final String algorithm;

    public HmacSigner(String secret) {
        this(secret, "HmacSHA256");
    }

    public HmacSigner(String secret, String algorithm) {
        this.key = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        this.algorithm = algorithm == null ? "HmacSHA256" : algorithm;
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    public String signBase64Url(String payload) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key, algorithm));
            byte[] sig = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC sign error", e);
        }
    }

    public boolean verifyBase64Url(String payload, String signature) {
        try {
            String expected = signBase64Url(payload);
            return constantTimeEquals(expected, signature);
        } catch (Exception e) {
            return false;
        }
    }
}
