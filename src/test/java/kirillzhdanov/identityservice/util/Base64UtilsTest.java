package kirillzhdanov.identityservice.util;

import kirillzhdanov.identityservice.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Base64UtilsTest {

    private String basic(String raw) {
        String b64 = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return "Basic " + b64;
    }

    @Test
    @DisplayName("Decode Basic header: username and password")
    void decode_username_and_password() {
        String header = basic("alice:secret");
        LoginRequest req = Base64Utils.getUsernameAndPassword(header);
        assertEquals("alice", req.getUsername());
        assertEquals("secret", req.getPassword());
    }

    @Test
    @DisplayName("Decode Basic header: username only (no colon) -> empty password")
    void decode_username_only() {
        String header = basic("bob");
        LoginRequest req = Base64Utils.getUsernameAndPassword(header);
        assertEquals("bob", req.getUsername());
        assertEquals("", req.getPassword());
    }

    @Test
    @DisplayName("Decode Basic header: empty password after colon")
    void decode_empty_password_after_colon() {
        String header = basic("carol:");
        LoginRequest req = Base64Utils.getUsernameAndPassword(header);
        assertEquals("carol", req.getUsername());
        assertEquals("", req.getPassword());
    }

    @Test
    @DisplayName("Invalid Base64 payload -> IllegalArgumentException from decoder")
    void invalid_base64_throws() {
        String header = "Basic !!!not-base64!!!";
        assertThrows(IllegalArgumentException.class, () -> Base64Utils.getUsernameAndPassword(header));
    }

    @Test
    @DisplayName("Header shorter than 'Basic ' prefix -> StringIndexOutOfBoundsException")
    void too_short_header_throws() {
        String header = "Bas"; // method substrings without checking startsWith
        assertThrows(StringIndexOutOfBoundsException.class, () -> Base64Utils.getUsernameAndPassword(header));
    }
}
