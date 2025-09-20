package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.dto.EmailVerificationRequest;
import kirillzhdanov.identityservice.dto.EmailVerifiedResponse;
import kirillzhdanov.identityservice.dto.UpdateUserRequest;
import kirillzhdanov.identityservice.dto.UserResponse;
import kirillzhdanov.identityservice.exception.BadRequestException;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailService mailService;

    @InjectMocks
    private UserProfileService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("user1");
        user.setFirstName("Иван");
        user.setLastName("Иванов");
        user.setEmail("old@example.com");
        user.setEmailVerified(true);
    }

    @Test
    @DisplayName("checkEmailVerified: true, когда email совпадает и user.emailVerified=true")
    void checkEmailVerified_true_whenMatchedAndVerified() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        EmailVerifiedResponse resp = service.checkEmailVerified("user1", "OLD@example.com");
        assertThat(resp.isVerified()).isTrue();
    }

    @Test
    @DisplayName("checkEmailVerified: false, если email не совпадает")
    void checkEmailVerified_false_whenEmailDifferent() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        EmailVerifiedResponse resp = service.checkEmailVerified("user1", "other@example.com");
        assertThat(resp.isVerified()).isFalse();
    }

    @Test
    @DisplayName("sendVerificationCode: бросает BadRequest, если email пустой")
    void sendVerificationCode_emptyEmail() {
        assertThatThrownBy(() -> service.sendVerificationCode("user1", " "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email обязателен");
    }

    @Test
    @DisplayName("sendVerificationCode: сохраняет pendingEmail/код и шлёт письмо")
    void sendVerificationCode_ok() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.sendVerificationCode("user1", "new@example.com");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPendingEmail()).isEqualTo("new@example.com");
        assertThat(saved.isEmailVerified()).isFalse();
        assertThat(saved.getEmailVerificationCode()).isNotBlank();
        assertThat(saved.getEmailVerificationExpiresAt()).isAfter(LocalDateTime.now().minusSeconds(1));
        verify(mailService).sendEmailVerificationCode(eq("new@example.com"), anyString());
    }

    @Test
    @DisplayName("verifyCode: успешное подтверждение обновляет email и флаги")
    void verifyCode_success() {
        user.setPendingEmail("new@example.com");
        user.setEmailVerified(false);
        user.setEmailVerificationCode("123456");
        user.setEmailVerificationExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        EmailVerifiedResponse resp = service.verifyCode("user1",
                new EmailVerificationRequest("new@example.com", "123456"));

        assertThat(resp.isVerified()).isTrue();
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getPendingEmail()).isNull();
        assertThat(user.getEmailVerificationCode()).isNull();
        assertThat(user.getEmailVerificationExpiresAt()).isNull();
        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("verifyCode: истёкший код -> BadRequest")
    void verifyCode_expired() {
        user.setPendingEmail("new@example.com");
        user.setEmailVerificationCode("123456");
        user.setEmailVerificationExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.verifyCode("user1",
                new EmailVerificationRequest("new@example.com", "123456")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Срок действия кода истёк");
    }

    @Test
    @DisplayName("updateProfile: обновляет поля и не перезаписывает email, если он уже установлен")
    void updateProfile_updatesFields_emailOnlyIfNull() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest req = new UpdateUserRequest();
        req.setFirstName("Пётр");
        req.setLastName("Петров");
        req.setPatronymic("Сергеевич");
        req.setDateOfBirth(LocalDate.of(1990, 1, 2));
        req.setPhone("+70000000000");
        req.setEmail("new@example.com"); // должен игнорироваться, т.к. email уже есть

        UserResponse resp = service.updateProfile("user1", req);

        assertThat(resp.getFirstName()).isEqualTo("Пётр");
        assertThat(resp.getLastName()).isEqualTo("Петров");
        assertThat(resp.getPatronymic()).isEqualTo("Сергеевич");
        assertThat(resp.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 2));
        assertThat(resp.getPhone()).isEqualTo("+70000000000");
        // email остался старым
        assertThat(resp.getEmail()).isEqualTo("old@example.com");
    }

    @Test
    @DisplayName("updateProfile: ставит email, если ранее он был null")
    void updateProfile_setsEmailIfNull() {
        user.setEmail(null);
        user.setEmailVerified(false);
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest req = new UpdateUserRequest();
        req.setEmail("new@example.com");

        UserResponse resp = service.updateProfile("user1", req);
        assertThat(resp.getEmail()).isEqualTo("new@example.com");
        assertThat(resp.getEmailVerified()).isFalse();
    }
}
