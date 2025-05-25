package kirillzhdanov.identityservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.BotRegistrationRequest;
import kirillzhdanov.identityservice.dto.MessageResponse;
import kirillzhdanov.identityservice.service.BotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bot")
@Tag(name = "Telegram-боты", description = "API для управления Telegram-ботами")
public class BotController {

    @Autowired
    private BotService botService;

    @Operation(summary = "Регистрация бота", description = "Регистрирует новый Telegram-бот в системе")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Бот успешно зарегистрирован",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
            @ApiResponse(responseCode = "409", description = "Бот с таким токеном уже существует")
    })
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> registerBot(@Valid @RequestBody BotRegistrationRequest request) {
        MessageResponse response = botService.registerBot(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Запуск бота", description = "Запускает зарегистрированный Telegram-бот")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Бот успешно запущен",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
            @ApiResponse(responseCode = "404", description = "Бот не найден")
    })
    @PostMapping("/start")
    public ResponseEntity<MessageResponse> startBot(@Valid @RequestBody BotRegistrationRequest request) {
        MessageResponse response = botService.startBot(request);
        return ResponseEntity.ok(response);
    }
}
