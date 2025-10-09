package kirillzhdanov.identityservice.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.util.HmacSigner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CtxCookieFilterTest {

    private static String buildCtxValue(Long masterId, Long brandId, Long pickupPointId, String secret) {
        long issuedAt = System.currentTimeMillis();
        String json = "{" +
                (masterId != null ? "\"masterId\":" + masterId + "," : "") +
                (brandId != null ? "\"brandId\":" + brandId + "," : "") +
                (pickupPointId != null ? "\"pickupPointId\":" + pickupPointId + "," : "") +
                "\"issuedAt\":" + issuedAt +
                "}";
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        HmacSigner signer = new HmacSigner(secret);
        String sig = signer.signBase64Url(payload);
        return payload + "." + sig;
    }

    @Test
    @DisplayName("Фильтр устанавливает контекст при валидной подписи ctx")
    void filter_setsContext_onValidCookie() throws ServletException, IOException, NoSuchFieldException, IllegalAccessException {
        CtxCookieFilter filter = new CtxCookieFilter();
        // проставим секрет через reflection (используется значение по умолчанию change-me, но зададим явно)
        var f = CtxCookieFilter.class.getDeclaredField("secret");
        f.setAccessible(true);
        f.set(filter, "change-me");

        String value = buildCtxValue(100L, 200L, 300L, "change-me");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("ctx", value));
        MockHttpServletResponse res = new MockHttpServletResponse();

        AtomicReference<ContextResolver.Ctx> seen = new AtomicReference<>();
        FilterChain chain = (request, response) -> {
            seen.set(ContextResolver.current());
        };

        filter.doFilter(req, res, chain);

        ContextResolver.Ctx ctx = seen.get();
        assertThat(ctx).isNotNull();
        assertThat(ctx.masterId()).isEqualTo(100L);
        assertThat(ctx.brandId()).isEqualTo(200L);
        assertThat(ctx.pickupPointId()).isEqualTo(300L);
    }

    @Test
    @DisplayName("Фильтр игнорирует ctx при неверной подписи")
    void filter_ignores_onBadSignature() throws ServletException, IOException, NoSuchFieldException, IllegalAccessException {
        CtxCookieFilter filter = new CtxCookieFilter();
        var f = CtxCookieFilter.class.getDeclaredField("secret");
        f.setAccessible(true);
        f.set(filter, "change-me");

        // подпишем другим секретом
        String value = buildCtxValue(1L, 2L, 3L, "wrong-secret");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("ctx", value));
        MockHttpServletResponse res = new MockHttpServletResponse();

        AtomicReference<ContextResolver.Ctx> seen = new AtomicReference<>();
        FilterChain chain = (request, response) -> {
            seen.set(ContextResolver.current());
        };

        filter.doFilter(req, res, chain);

        assertThat(seen.get()).isNull();
    }

    @Test
    @DisplayName("Фильтр игнорирует ctx при некорректном формате: без точки")
    void filter_ignores_onNoDot() throws Exception {
        CtxCookieFilter filter = new CtxCookieFilter();
        var f = CtxCookieFilter.class.getDeclaredField("secret");
        f.setAccessible(true);
        f.set(filter, "change-me");

        MockHttpServletRequest req = new MockHttpServletRequest();
        // value без точки
        req.setCookies(new Cookie("ctx", "invalid-format"));
        MockHttpServletResponse res = new MockHttpServletResponse();

        AtomicReference<ContextResolver.Ctx> seen = new AtomicReference<>();
        FilterChain chain = (request, response) -> seen.set(ContextResolver.current());

        filter.doFilter(req, res, chain);
        assertThat(seen.get()).isNull();
    }

    @Test
    @DisplayName("Фильтр игнорирует ctx при невалидном Base64 payload")
    void filter_ignores_onBadBase64() throws Exception {
        CtxCookieFilter filter = new CtxCookieFilter();
        var f = CtxCookieFilter.class.getDeclaredField("secret");
        f.setAccessible(true);
        f.set(filter, "change-me");

        // Подпишем не-Base64 строку (verify пройдёт, но decode упадёт и будет проигнорировано)
        String payload = "%%%not-base64%%%";
        HmacSigner signer = new HmacSigner("change-me");
        String sig = signer.signBase64Url(payload);
        String value = payload + "." + sig;

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("ctx", value));
        MockHttpServletResponse res = new MockHttpServletResponse();

        AtomicReference<ContextResolver.Ctx> seen = new AtomicReference<>();
        FilterChain chain = (request, response) -> seen.set(ContextResolver.current());

        filter.doFilter(req, res, chain);
        assertThat(seen.get()).isNull();
    }

    @Test
    @DisplayName("Фильтр очищает ThreadLocal контекст в finally даже при исключении в цепочке")
    void filter_clears_context_onFinally() throws Exception {
        CtxCookieFilter filter = new CtxCookieFilter();
        var f = CtxCookieFilter.class.getDeclaredField("secret");
        f.setAccessible(true);
        f.set(filter, "change-me");

        String value = buildCtxValue(1L, 2L, 3L, "change-me");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("ctx", value));
        MockHttpServletResponse res = new MockHttpServletResponse();

        // Цепочка бросает исключение
        FilterChain chain = (request, response) -> {
            throw new ServletException("boom");
        };

        try {
            filter.doFilter(req, res, chain);
        } catch (ServletException ignored) {
        }
        // После выполнения фильтра контекст должен быть очищен
        assertThat(ContextResolver.current()).isNull();
    }
}
