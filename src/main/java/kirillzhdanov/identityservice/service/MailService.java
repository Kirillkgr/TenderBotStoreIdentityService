package kirillzhdanov.identityservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {
    private final ThreadLocal<JavaMailSender> mailSender = new ThreadLocal<>();

    @Value("${app.mail.from:no-reply@kirillkgr.ru}")
    private String fromAddress;

    public void sendEmailVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Подтверждение email");
        message.setText("Ваш код подтверждения: " + code + "\nКод действителен 10 минут.");
        mailSender.get().send(message);
    }
}
