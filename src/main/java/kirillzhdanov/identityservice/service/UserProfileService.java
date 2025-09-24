package kirillzhdanov.identityservice.service;

import jakarta.transaction.Transactional;
import kirillzhdanov.identityservice.dto.*;
import kirillzhdanov.identityservice.exception.BadRequestException;
import kirillzhdanov.identityservice.model.StorageFile;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.repository.StorageFileRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final MailService mailService;
    private final MediaService mediaService;
    private final StorageFileRepository storageFileRepository;
    private final S3StorageService s3StorageService;

    @Transactional
    public EmailVerifiedResponse checkEmailVerified(String username, String email) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Пользователь не найден"));
        boolean verified = user.isEmailVerified() && email != null && email.equalsIgnoreCase(user.getEmail());
        return EmailVerifiedResponse.builder().verified(verified).build();
    }

    @Transactional
    public void sendVerificationCode(String username, String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email обязателен");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Пользователь не найден"));
        // генерируем 6-значный код
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        user.setPendingEmail(email);
        user.setEmailVerified(false);
        user.setEmailVerificationCode(code);
        user.setEmailVerificationExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);
        mailService.sendEmailVerificationCode(email, code);
    }

    @Transactional
    public EmailVerifiedResponse verifyCode(String username, EmailVerificationRequest req) {
        if (req.getEmail() == null || req.getCode() == null) {
            throw new BadRequestException("Email и код обязательны");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Пользователь не найден"));
        if (user.getPendingEmail() == null || !req.getEmail().equalsIgnoreCase(user.getPendingEmail())) {
            throw new BadRequestException("Email не запрошен для подтверждения");
        }
        if (user.getEmailVerificationCode() == null || user.getEmailVerificationExpiresAt() == null) {
            throw new BadRequestException("Код не запрошен");
        }
        if (LocalDateTime.now().isAfter(user.getEmailVerificationExpiresAt())) {
            throw new BadRequestException("Срок действия кода истёк");
        }
        if (!req.getCode().trim().equals(user.getEmailVerificationCode())) {
            throw new BadRequestException("Неверный код");
        }
        // подтверждаем
        user.setEmail(user.getPendingEmail());
        user.setPendingEmail(null);
        user.setEmailVerified(true);
        user.setEmailVerificationCode(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);
        return EmailVerifiedResponse.builder().verified(true).build();
    }

    @Transactional
    public UserResponse updateProfile(String username, UpdateUserRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Пользователь не найден"));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPatronymic() != null) user.setPatronymic(request.getPatronymic());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getEmail() != null && user.getEmail() == null) {
            user.setEmail(request.getEmail());
            user.setEmailVerified(false);
        }// Добавляем email только если он не указан и помечаем что он не подтвержден
        // Email меняется только через успешную верификацию (verifyCode), здесь не трогаем

        user = userRepository.save(user);

        // Сформируем avatarUrl аналогично остальным местам: пробуем получить presigned URL, иначе - служебный URL
        String avatarUrl = user.getAvatarUrl();
        try {
            String key = mediaService.getUserAvatarKey(String.valueOf(user.getId()));
            String presigned = s3StorageService
                    .buildPresignedGetUrl(key, java.time.Duration.ofHours(12))
                    .orElse(avatarUrl);
            if (presigned != null) {
                avatarUrl = presigned;
            }
        } catch (Exception ignored) {
            // если ключа нет — оставляем как есть (null или "/user/v1/avatar" если ранее установлен)
        }

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .patronymic(user.getPatronymic())
                .dateOfBirth(user.getDateOfBirth())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(avatarUrl)
                .emailVerified(user.isEmailVerified())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(java.util.stream.Collectors.toSet()))
                .brands(user.getBrands().stream()
                        .map(b -> kirillzhdanov.identityservice.dto.BrandDto.builder().id(b.getId()).name(b.getName()).build())
                        .collect(java.util.stream.Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Transactional
    public AvatarUploadResponse uploadAvatar(String username, MultipartFile file) throws java.io.IOException {
        if (file == null || file.isEmpty()) throw new BadRequestException("Пустой файл");
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Пользователь не найден"));
        String userId = String.valueOf(user.getId());
        var res = mediaService.uploadUserAvatar(userId, file.getBytes(), file.getContentType());
        String key = res.get("key");

        // upsert storage record for this user's avatar
        StorageFile sf = storageFileRepository.findByOwnerTypeAndOwnerId("USER", user.getId())
                .stream()
                .filter(f -> "USER_AVATAR".equals(f.getPurpose()))
                .findFirst()
                .orElseGet(() -> StorageFile.builder()
                        .ownerType("USER")
                        .ownerId(user.getId())
                        .purpose("USER_AVATAR")
                        .createdAt(LocalDateTime.now())
                        .build());
        sf.setPath(key);
        sf.setUpdatedAt(LocalDateTime.now());
        storageFileRepository.save(sf);

        // For privacy, expose backend endpoint as avatarUrl
        String servedUrl = "/user/v1/avatar";
        user.setAvatarUrl(servedUrl);
        userRepository.save(user);

        String presigned = s3StorageService.buildPresignedGetUrl(key, java.time.Duration.ofHours(12))
                .orElse(servedUrl);
        return AvatarUploadResponse.builder()
                .avatarUrl(presigned)
                .key(key)
                .build();
    }

    @Transactional
    public byte[] getAvatarBytes(Long userId) {
        String key = mediaService.getUserAvatarKey(String.valueOf(userId));
        return s3StorageService.getObjectBytes(key);
    }

    @Transactional
    public byte[] getAvatarBytesByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Пользователь не найден"));
        String key = mediaService.getUserAvatarKey(String.valueOf(user.getId()));
        return s3StorageService.getObjectBytes(key);
    }
}
