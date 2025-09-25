package kirillzhdanov.identityservice.notification.longpoll;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Глобальный перехватчик исключений для LongPollController,
 * чтобы исключить любые 500 и отдавать безопасный idle-ответ 204.
 */
@Slf4j
@RestControllerAdvice(assignableTypes = LongPollController.class)
public class LongPollExceptionAdvice {

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Void> handleAny(Throwable ex) {
        try {
            log.error("LongPoll: unhandled exception", ex);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        // Всегда безопасный idle-ответ вместо 500
        return ResponseEntity.noContent().build();
    }
}
