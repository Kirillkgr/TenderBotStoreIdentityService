# Swagger / OpenAPI

## Группы

- public — публичные ручки без авторизации. Настроено через `OpenApiConfig.publicApi()` и `removeSecurityCustomizer()`.
- secure — все защищённые ручки, кроме инвентаря. Настроено через `OpenApiConfig.secureApi()`.
- inventory — ручки инвентаря по префиксу `/auth/v1/inventory/**`. Настроено через `OpenApiConfig.inventoryApi()`.

## Как добавить новую ручку в группу

- Публичная ручка: убедитесь, что путь включён в `publicApi.pathsToMatch(...)` и исключён из `secureApi.pathsToExclude(...)`.
- Инвентарь: путь должен начинаться с `/auth/v1/inventory/**`.
- Остальные — автоматически попадут в `secure`.

## Аннотации контроллеров

- Используйте `@Operation(summary, description)` и `@Tag`.
- Описывайте параметры `@Parameter` для `@PathVariable` и `@RequestParam`.
- Ошибки указывайте через `@ApiResponses` с кодами 400/401/403/404/409/500, например:

```java
@ApiResponses({
  @ApiResponse(responseCode = "200", description = "Успешно"),
  @ApiResponse(responseCode = "400", description = "Неверный запрос",
      content = @Content(mediaType = "application/json",
          schema = @Schema(implementation = java.util.Map.class)))
})
```

Глобальный обработчик `GlobalExceptionHandler` возвращает JSON c полем `message` и, для 400, деталями валидации.

## Полезные классы

- `OpenApiConfig` — конфигурация Swagger групп и секьюрности.
- `GlobalExceptionHandler` — формат ошибок.
- Контроллеры инвентаря в пакете `controller.inventory` — примеры корректной документации.
